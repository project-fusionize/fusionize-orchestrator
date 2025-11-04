package dev.fusionize.script.engine;

import javax.script.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Secure, generic ScriptRunner for Kotlin, Groovy, and GraalJS.
 * Provides sandboxing via classloader isolation and timeout control.
 */
public class ScriptRunner {

    private static final long DEFAULT_TIMEOUT_MS = 30_000; // 30 seconds
    private final ScriptEngine engine;

    public ScriptRunner(ScriptRunnerEngine engineType) {
        ScriptEngineManager manager =
                new ScriptEngineManager(new RestrictedClassLoader(ClassLoader.getSystemClassLoader()));
        this.engine = manager.getEngineByName(engineType.getName());
        if (this.engine == null) {
            throw new IllegalArgumentException("Script engine not found: " + engineType.getName());
        }
    }

    public Object executeFunction(String script, String functionName, Object... args)
            throws ScriptException, NoSuchMethodException {
        return executeFunctionWithTimeout(script, functionName, DEFAULT_TIMEOUT_MS, args);
    }

    public Object executeFunctionWithTimeout(String script, String functionName, long timeoutMs, Object... args)
            throws ScriptException, NoSuchMethodException {

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "sandboxed-script-thread");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

        try {
            Future<Object> future = executor.submit(() -> {
                Thread.currentThread().setContextClassLoader(engine.getClass().getClassLoader());
                engine.eval(script);

                if (engine instanceof Invocable invocable) {
                    return invocable.invokeFunction(functionName, args);
                }
                throw new UnsupportedOperationException("Engine does not support function invocation.");
            });

            return future.get(timeoutMs, TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            futureCancelSafe(executor);
            throw new ScriptException("Script execution timed out after " + timeoutMs + " ms");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ScriptException se) throw se;
            if (cause instanceof NoSuchMethodException nsm) throw nsm;
            throw new ScriptException("Script execution failed: " + cause.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptException("Script execution interrupted");
        } finally {
            executor.shutdownNow();
        }
    }

    private void futureCancelSafe(ExecutorService executor) {
        try {
            executor.shutdownNow();
        } catch (Exception ignored) {
        }
    }

    /**
     * Restrictive ClassLoader that only allows whitelisted packages.
     * Prevents scripts from loading sensitive system classes or accessing files/network.
     */
    public static class RestrictedClassLoader extends ClassLoader {

        // Safe packages required by engines
        private static final List<String> ALLOWED_PACKAGES = List.of(
                "java.lang.",
                "java.util.",
                "javax.script.",
                "org.graalvm.",        // Graal.js
                "com.oracle.truffle.", // Graal internals
                "org.codehaus.groovy.",// Groovy
                "groovy.",             // Groovy stdlib
                "kotlin.",             // Kotlin stdlib
                "org.jetbrains.kotlin."// Kotlin internals
        );

        // Sensitive packages explicitly blocked
        private static final List<String> BLOCKED_PACKAGES = List.of(
                "java.io.",
                "java.nio.",
                "java.net.",
                "java.lang.reflect.",
                "java.lang.management.",
                "java.security.",
                "sun.",
                "com.sun."
        );

        public RestrictedClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            for (String blocked : BLOCKED_PACKAGES) {
                if (name.startsWith(blocked)) {
                    throw new ClassNotFoundException("Access denied for sensitive class: " + name);
                }
            }
            for (String allowed : ALLOWED_PACKAGES) {
                if (name.startsWith(allowed)) {
                    return super.loadClass(name, resolve);
                }
            }
            throw new ClassNotFoundException("Access denied for class: " + name);
        }
    }
}
