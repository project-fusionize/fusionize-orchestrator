package dev.fusionize.script.engine;

import javax.script.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * Secure, generic ScriptRunner for executing Kotlin, Groovy, and GraalJS scripts
 * with sandboxing and timeout control.
 *
 * <p>This class provides a secure execution environment for scripts by:
 * <ul>
 *   <li>Using a restricted classloader that blocks access to sensitive system classes</li>
 *   <li>Executing scripts in isolated daemon threads with minimum priority</li>
 *   <li>Enforcing configurable execution timeouts</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.KOTLIN);
 * Object result = runner.eval("2 + 2", null);
 * </pre>
 *
 * @author Fusionize Dev Team
 */
public class ScriptRunner {

    /** Default timeout for script execution (30 seconds) */
    private static final long DEFAULT_TIMEOUT_MS = 30_000;

    /** Thread name prefix for sandboxed script execution threads */
    private static final String THREAD_NAME = "sandboxed-script-thread";

    /** The underlying script engine (Kotlin, Groovy, or GraalJS) */
    private final ScriptEngine engine;

    /**
     * Creates a new ScriptRunner with the specified engine type.
     *
     * @param engineType the type of script engine to use
     * @throws IllegalArgumentException if the script engine cannot be found
     */
    public ScriptRunner(ScriptRunnerEngine engineType) {
        ScriptEngineManager manager = new ScriptEngineManager(
                new RestrictedClassLoader(ClassLoader.getSystemClassLoader()));
        this.engine = manager.getEngineByName(engineType.getName());

        if (this.engine == null) {
            throw new IllegalArgumentException("Script engine not found: " + engineType.getName());
        }

        Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        if(engineType == ScriptRunnerEngine.JS) {
            engineBindings.put("polyglot.js.allowHostAccess", true);
        }

    }

    /**
     * Executes a function defined in the script with the default timeout.
     *
     * @param script the script source code containing the function definition
     * @param functionName the name of the function to invoke
     * @param args arguments to pass to the function
     * @return the result returned by the function
     * @throws ScriptException if script evaluation or execution fails
     * @throws NoSuchMethodException if the specified function does not exist
     */
    public Object executeFunction(String script, String functionName, Object... args)
            throws ScriptException, NoSuchMethodException {
        return executeFunctionWithTimeout(script, functionName, DEFAULT_TIMEOUT_MS, args);
    }

    /**
     * Executes a function defined in the script with a custom timeout.
     *
     * <p>The script is first evaluated to define the function, then the function
     * is invoked with the provided arguments.
     *
     * @param script the script source code containing the function definition
     * @param functionName the name of the function to invoke
     * @param timeoutMs maximum execution time in milliseconds
     * @param args arguments to pass to the function
     * @return the result returned by the function
     * @throws ScriptException if script evaluation/execution fails or times out
     * @throws NoSuchMethodException if the specified function does not exist
     */
    public Object executeFunctionWithTimeout(String script, String functionName, long timeoutMs, Object... args)
            throws ScriptException, NoSuchMethodException {

        return executeInSandbox(() -> {
            // Evaluate the script to define functions
            engine.eval(script);

            // Invoke the specified function
            if (engine instanceof Invocable invocable) {
                return invocable.invokeFunction(functionName, args);
            }
            throw new UnsupportedOperationException(
                    "Engine does not support function invocation: " + engine.getClass().getName());
        }, timeoutMs);
    }

    /**
     * Evaluates a script with the default timeout.
     *
     * @param script the script source code to evaluate
     * @param variables variables to bind in the script context (can be null)
     * @return the result of the script evaluation
     * @throws ScriptException if script evaluation fails or times out
     */
    public Object eval(String script, Map<String, Object> variables) throws ScriptException, NoSuchMethodException {
        return evalWithTimeout(script, variables, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Evaluates a script with a custom timeout and variable bindings.
     *
     * @param script the script source code to evaluate
     * @param variables variables to bind in the script context (can be null)
     * @param timeoutMs maximum execution time in milliseconds
     * @return the result of the script evaluation
     * @throws ScriptException if script evaluation fails or times out
     */
    public Object evalWithTimeout(String script, Map<String, Object> variables, long timeoutMs)
            throws ScriptException, NoSuchMethodException {

        return executeInSandbox(() -> {
            // Create and populate bindings with provided variables
            Bindings bindings = engine.createBindings();
            if (variables != null) {
                bindings.putAll(variables);
            }
            return engine.eval(script, bindings);
        }, timeoutMs);
    }

    /**
     * Core method that executes script operations in a sandboxed environment.
     *
     * <p>This method handles:
     * <ul>
     *   <li>Creating an isolated executor service with a daemon thread</li>
     *   <li>Setting appropriate thread context classloader</li>
     *   <li>Enforcing timeout constraints</li>
     *   <li>Proper exception handling and resource cleanup</li>
     * </ul>
     *
     * @param task the script execution task to run
     * @param timeoutMs maximum execution time in milliseconds
     * @return the result of the script execution
     * @throws ScriptException if execution fails or times out
     * @throws NoSuchMethodException if a required method is not found (propagated from task)
     */
    private Object executeInSandbox(ScriptTask task, long timeoutMs)
            throws ScriptException, NoSuchMethodException {

        ExecutorService executor = createSandboxedExecutor();

        try {
            Future<Object> future = executor.submit(() -> {
                // Set context classloader to the engine's classloader for proper isolation
                Thread.currentThread().setContextClassLoader(engine.getClass().getClassLoader());
                return task.execute();
            });

            // Wait for completion with timeout
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            // Attempt to cancel the running task
            shutdownExecutorSafely(executor);
            throw new ScriptException("Script execution timed out after " + timeoutMs + " ms");

        } catch (ExecutionException e) {
            // Unwrap and rethrow the original exception
            Throwable cause = e.getCause();
            if (cause instanceof ScriptException se) {
                throw se;
            }
            if (cause instanceof NoSuchMethodException nsm) {
                throw nsm;
            }
            if (cause instanceof RuntimeException re) {
                throw new ScriptException("Script execution failed: " + re.getMessage());
            }
            throw new ScriptException("Script execution failed: " +
                    (cause != null ? cause.getMessage() : e.getMessage()));

        } catch (InterruptedException e) {
            // Restore interrupt status and throw
            Thread.currentThread().interrupt();
            throw new ScriptException("Script execution interrupted");

        } finally {
            // Always attempt cleanup
            shutdownExecutorSafely(executor);
        }
    }

    /**
     * Creates a single-threaded executor service configured for sandboxed script execution.
     *
     * <p>The created thread is:
     * <ul>
     *   <li>A daemon thread (won't prevent JVM shutdown)</li>
     *   <li>Set to minimum priority (reduces impact on other operations)</li>
     *   <li>Named for easy identification in thread dumps</li>
     * </ul>
     *
     * @return a configured executor service
     */
    private ExecutorService createSandboxedExecutor() {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, THREAD_NAME);
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    /**
     * Safely attempts to shutdown the executor service, suppressing any exceptions.
     *
     * <p>This method attempts immediate shutdown using {@code shutdownNow()},
     * which attempts to stop actively executing tasks.
     *
     * @param executor the executor service to shutdown
     */
    private void shutdownExecutorSafely(ExecutorService executor) {
        try {
            executor.shutdownNow();
        } catch (Exception ignored) {
            // Suppress shutdown exceptions - best effort only
        }
    }

    /**
     * Functional interface for script execution tasks that can throw checked exceptions.
     */
    @FunctionalInterface
    private interface ScriptTask {
        /**
         * Executes the script task.
         *
         * @return the result of script execution
         * @throws Exception if execution fails
         */
        Object execute() throws Exception;
    }

    /**
     * Restrictive ClassLoader that only allows whitelisted packages.
     *
     * <p>This classloader provides security by:
     * <ul>
     *   <li>Explicitly blocking sensitive packages (I/O, networking, reflection, etc.)</li>
     *   <li>Only allowing essential packages required by script engines</li>
     *   <li>Throwing ClassNotFoundException for all other class loads</li>
     * </ul>
     *
     * <p>This prevents scripts from accessing the filesystem, network, or using
     * reflection to bypass security restrictions.
     */
    public static class RestrictedClassLoader extends ClassLoader {

        /**
         * Safe packages that scripts are allowed to use.
         * Includes standard library classes and script engine internals.
         */
        private static final List<String> ALLOWED_PACKAGES = List.of(
                "java.lang.",           // Core Java classes (String, Integer, etc.)
                "java.util.",           // Collections and utilities
                "javax.script.",        // Script engine API
                "org.graalvm.",         // GraalVM JavaScript engine
                "com.oracle.truffle.",  // Truffle framework (used by GraalVM)
                "org.codehaus.groovy.", // Groovy implementation
                "groovy.",              // Groovy standard library
                "kotlin.",              // Kotlin standard library
                "org.jetbrains.kotlin." // Kotlin runtime internals
        );

        /**
         * Sensitive packages that are explicitly blocked for security.
         * Scripts cannot access these packages to prevent system access.
         */
        private static final List<String> BLOCKED_PACKAGES = List.of(
                "java.io.",              // File I/O operations
                "java.nio.",             // New I/O (file channels, buffers)
                "java.net.",             // Network operations
                "java.lang.reflect.",    // Reflection API (could bypass security)
                "java.lang.management.", // JVM management
                "java.security.",        // Security management
                "sun.",                  // Internal Sun/Oracle classes
                "com.sun."               // Internal Sun/Oracle classes
        );

        /**
         * Creates a new RestrictedClassLoader.
         *
         * @param parent the parent classloader for delegation
         */
        public RestrictedClassLoader(ClassLoader parent) {
            super(parent);
        }

        /**
         * Loads a class with security restrictions enforced.
         *
         * <p>The loading process:
         * <ol>
         *   <li>First checks if the class is in a blocked package (denied immediately)</li>
         *   <li>Then checks if the class is in an allowed package (delegates to parent)</li>
         *   <li>All other classes are denied by default (whitelist approach)</li>
         * </ol>
         *
         * @param name the fully qualified class name
         * @param resolve if true, resolve the class
         * @return the loaded class
         * @throws ClassNotFoundException if the class is blocked or not whitelisted
         */
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // Check blocked packages first (deny list)
            for (String blocked : BLOCKED_PACKAGES) {
                if (name.startsWith(blocked)) {
                    throw new ClassNotFoundException("Access denied for sensitive class: " + name);
                }
            }

            // Check allowed packages (allow list)
            for (String allowed : ALLOWED_PACKAGES) {
                if (name.startsWith(allowed)) {
                    return super.loadClass(name, resolve);
                }
            }

            // Default deny - class is neither blocked nor explicitly allowed
            throw new ClassNotFoundException("Access denied for class: " + name);
        }
    }
}