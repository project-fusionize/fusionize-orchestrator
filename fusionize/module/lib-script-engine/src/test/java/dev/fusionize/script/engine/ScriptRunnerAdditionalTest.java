package dev.fusionize.script.engine;

import org.junit.jupiter.api.Test;

import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScriptRunnerAdditionalTest {

    // ────────────────────────────────────────────────
    // ScriptRunnerEngine enum lookup
    // ────────────────────────────────────────────────

    @Test
    void shouldLookupEngineByName() {
        // setup
        // no setup needed

        // expectation
        var jsEngine = ScriptRunnerEngine.get("graal.js");
        var kotlinEngine = ScriptRunnerEngine.get("kotlin");
        var groovyEngine = ScriptRunnerEngine.get("groovy");
        var nullEngine = ScriptRunnerEngine.get("nonexistent");

        // validation
        assertThat(jsEngine).isEqualTo(ScriptRunnerEngine.JS);
        assertThat(kotlinEngine).isEqualTo(ScriptRunnerEngine.KOTLIN);
        assertThat(groovyEngine).isEqualTo(ScriptRunnerEngine.GROOVY);
        assertThat(nullEngine).isNull();
    }

    @Test
    void shouldReturnCorrectEngineNames() {
        // setup
        // no setup needed

        // expectation
        // no action needed

        // validation
        assertThat(ScriptRunnerEngine.JS.getName()).isEqualTo("graal.js");
        assertThat(ScriptRunnerEngine.KOTLIN.getName()).isEqualTo("kotlin");
        assertThat(ScriptRunnerEngine.GROOVY.getName()).isEqualTo("groovy");
    }

    @Test
    void shouldEnumerateAllEngineValues() {
        // setup
        // no setup needed

        // expectation
        var values = ScriptRunnerEngine.values();

        // validation
        assertThat(values).hasSize(3);
        assertThat(values).containsExactly(ScriptRunnerEngine.JS, ScriptRunnerEngine.KOTLIN, ScriptRunnerEngine.GROOVY);
    }

    // ────────────────────────────────────────────────
    // eval with variables — Groovy
    // ────────────────────────────────────────────────

    @Test
    void shouldEvalWithVariables_Groovy() throws ScriptException, NoSuchMethodException {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.GROOVY);
        Map<String, Object> vars = new HashMap<>();
        vars.put("x", 7);
        vars.put("y", 3);

        // expectation
        Object result = runner.eval("x + y", vars);

        // validation
        assertThat(result).isEqualTo(10);
    }

    // ────────────────────────────────────────────────
    // eval with variables — Kotlin
    // ────────────────────────────────────────────────

    @Test
    void shouldEvalWithVariables_Kotlin() throws ScriptException, NoSuchMethodException {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.KOTLIN);
        Map<String, Object> vars = new HashMap<>();
        vars.put("x", 7);
        vars.put("y", 3);

        // expectation
        Object result = runner.eval("x as Int + (y as Int)", vars);

        // validation
        assertThat(result).isEqualTo(10);
    }

    // ────────────────────────────────────────────────
    // eval with null variables map
    // ────────────────────────────────────────────────

    @Test
    void shouldEvalWithNullVariables() throws ScriptException, NoSuchMethodException {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.JS);

        // expectation
        Object result = runner.eval("5 * 4", null);

        // validation
        assertThat(result).isEqualTo(20);
    }

    // ────────────────────────────────────────────────
    // eval boolean expressions
    // ────────────────────────────────────────────────

    @Test
    void shouldEvalBooleanExpressionJS() throws ScriptException, NoSuchMethodException {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.JS);
        Map<String, Object> vars = new HashMap<>();
        vars.put("value", 15);

        // expectation
        Object result = runner.eval("value > 10", vars);

        // validation
        assertThat(result).isEqualTo(true);
    }

    @Test
    void shouldEvalFalseBooleanExpressionJS() throws ScriptException, NoSuchMethodException {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.JS);
        Map<String, Object> vars = new HashMap<>();
        vars.put("value", 5);

        // expectation
        Object result = runner.eval("value > 10", vars);

        // validation
        assertThat(result).isEqualTo(false);
    }

    // ────────────────────────────────────────────────
    // executeFunction with custom timeout
    // ────────────────────────────────────────────────

    @Test
    void shouldExecuteFunctionWithCustomTimeout_JS() throws ScriptException, NoSuchMethodException {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.JS);
        String script = "function multiply(a, b) { return a * b; }";

        // expectation
        Object result = runner.executeFunctionWithTimeout(script, "multiply", 5000, 6, 7);

        // validation
        assertThat(result).isEqualTo(42);
    }

    @Test
    void shouldExecuteFunctionWithCustomTimeout_Groovy() throws ScriptException, NoSuchMethodException {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.GROOVY);
        String script = "def multiply(a, b) { return a * b }";

        // expectation
        Object result = runner.executeFunctionWithTimeout(script, "multiply", 5000, 6, 7);

        // validation
        assertThat(result).isEqualTo(42);
    }

    // ────────────────────────────────────────────────
    // Error: script syntax error
    // ────────────────────────────────────────────────

    @Test
    void shouldThrowOnInvalidScript_JS() {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.JS);

        // expectation / validation
        assertThatThrownBy(() -> runner.eval("this is not valid javascript %%%", null))
                .isInstanceOf(ScriptException.class);
    }

    @Test
    void shouldThrowOnInvalidScript_Groovy() {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.GROOVY);

        // expectation / validation
        assertThatThrownBy(() -> runner.eval("def def def invalid{{{", null))
                .isInstanceOf(ScriptException.class);
    }

    // ────────────────────────────────────────────────
    // Error: NoSuchMethodException for missing function
    // ────────────────────────────────────────────────

    @Test
    void shouldThrowWhenFunctionNotFound_JS() {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.JS);
        String script = "function existing() { return 1; }";

        // expectation / validation
        assertThatThrownBy(() -> runner.executeFunction(script, "nonExistentFunction"))
                .isInstanceOf(NoSuchMethodException.class);
    }

    // ────────────────────────────────────────────────
    // Error: timeout on eval
    // ────────────────────────────────────────────────

    @Test
    void shouldThrowOnEvalTimeout_Groovy() {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.GROOVY);

        // expectation / validation
        assertThatThrownBy(() -> runner.evalWithTimeout("while(true) {}", null, 200))
                .isInstanceOf(ScriptException.class)
                .hasMessageContaining("timed out");
    }

    // ────────────────────────────────────────────────
    // Error: timeout on executeFunction
    // ────────────────────────────────────────────────

    @Test
    void shouldThrowOnExecuteFunctionTimeout() {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.JS);
        String script = "function loop() { while(true) {} }";

        // expectation / validation
        assertThatThrownBy(() -> runner.executeFunctionWithTimeout(script, "loop", 200))
                .isInstanceOf(ScriptException.class)
                .hasMessageContaining("timed out");
    }

    // ────────────────────────────────────────────────
    // RestrictedClassLoader — blocked packages
    // ────────────────────────────────────────────────

    @Test
    void shouldBlockJavaIOAccess() {
        // setup
        var loader = new ScriptRunner.RestrictedClassLoader(ClassLoader.getSystemClassLoader());

        // expectation / validation
        assertThatThrownBy(() -> loader.loadClass("java.io.File"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("Access denied for sensitive class");
    }

    @Test
    void shouldBlockJavaNioAccess() {
        // setup
        var loader = new ScriptRunner.RestrictedClassLoader(ClassLoader.getSystemClassLoader());

        // expectation / validation
        assertThatThrownBy(() -> loader.loadClass("java.nio.file.Files"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("Access denied for sensitive class");
    }

    @Test
    void shouldBlockJavaNetAccess() {
        // setup
        var loader = new ScriptRunner.RestrictedClassLoader(ClassLoader.getSystemClassLoader());

        // expectation / validation
        assertThatThrownBy(() -> loader.loadClass("java.net.URL"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("Access denied for sensitive class");
    }

    @Test
    void shouldBlockReflectionAccess() {
        // setup
        var loader = new ScriptRunner.RestrictedClassLoader(ClassLoader.getSystemClassLoader());

        // expectation / validation
        assertThatThrownBy(() -> loader.loadClass("java.lang.reflect.Method"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("Access denied for sensitive class");
    }

    @Test
    void shouldBlockSunPackageAccess() {
        // setup
        var loader = new ScriptRunner.RestrictedClassLoader(ClassLoader.getSystemClassLoader());

        // expectation / validation
        assertThatThrownBy(() -> loader.loadClass("sun.misc.Unsafe"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("Access denied for sensitive class");
    }

    @Test
    void shouldBlockComSunPackageAccess() {
        // setup
        var loader = new ScriptRunner.RestrictedClassLoader(ClassLoader.getSystemClassLoader());

        // expectation / validation
        assertThatThrownBy(() -> loader.loadClass("com.sun.management.HotSpotDiagnosticMXBean"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("Access denied for sensitive class");
    }

    @Test
    void shouldBlockJavaSecurityAccess() {
        // setup
        var loader = new ScriptRunner.RestrictedClassLoader(ClassLoader.getSystemClassLoader());

        // expectation / validation
        assertThatThrownBy(() -> loader.loadClass("java.security.AccessController"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("Access denied for sensitive class");
    }

    @Test
    void shouldBlockJavaLangManagementAccess() {
        // setup
        var loader = new ScriptRunner.RestrictedClassLoader(ClassLoader.getSystemClassLoader());

        // expectation / validation
        assertThatThrownBy(() -> loader.loadClass("java.lang.management.ManagementFactory"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("Access denied for sensitive class");
    }

    // ────────────────────────────────────────────────
    // RestrictedClassLoader — allowed packages
    // ────────────────────────────────────────────────

    @Test
    void shouldAllowJavaLangClasses() throws ClassNotFoundException {
        // setup
        var loader = new ScriptRunner.RestrictedClassLoader(ClassLoader.getSystemClassLoader());

        // expectation
        Class<?> clazz = loader.loadClass("java.lang.String");

        // validation
        assertThat(clazz).isEqualTo(String.class);
    }

    @Test
    void shouldAllowJavaUtilClasses() throws ClassNotFoundException {
        // setup
        var loader = new ScriptRunner.RestrictedClassLoader(ClassLoader.getSystemClassLoader());

        // expectation
        Class<?> clazz = loader.loadClass("java.util.HashMap");

        // validation
        assertThat(clazz).isEqualTo(HashMap.class);
    }

    @Test
    void shouldAllowJavaxScriptClasses() throws ClassNotFoundException {
        // setup
        var loader = new ScriptRunner.RestrictedClassLoader(ClassLoader.getSystemClassLoader());

        // expectation
        Class<?> clazz = loader.loadClass("javax.script.ScriptEngine");

        // validation
        assertThat(clazz).isNotNull();
    }

    // ────────────────────────────────────────────────
    // RestrictedClassLoader — non-allowed, non-blocked
    // ────────────────────────────────────────────────

    @Test
    void shouldDenyUnknownPackage() {
        // setup
        var loader = new ScriptRunner.RestrictedClassLoader(ClassLoader.getSystemClassLoader());

        // expectation / validation
        assertThatThrownBy(() -> loader.loadClass("some.random.package.SomeClass"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("Access denied for class");
    }

    // ────────────────────────────────────────────────
    // Security: scripts cannot access filesystem
    // ────────────────────────────────────────────────

    @Test
    void shouldBlockFileAccessFromJSScript() {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.JS);

        // expectation / validation
        assertThatThrownBy(() -> runner.eval("var File = Java.type('java.io.File'); new File('/tmp/test');", null))
                .isInstanceOf(Exception.class);
    }

    // ────────────────────────────────────────────────
    // eval with empty variables map
    // ────────────────────────────────────────────────

    @Test
    void shouldEvalWithEmptyVariablesMap() throws ScriptException, NoSuchMethodException {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.JS);
        Map<String, Object> vars = new HashMap<>();

        // expectation
        Object result = runner.eval("2 + 3", vars);

        // validation
        assertThat(result).isEqualTo(5);
    }

    // ────────────────────────────────────────────────
    // evalWithTimeout with variables
    // ────────────────────────────────────────────────

    @Test
    void shouldEvalWithTimeoutAndVariables() throws ScriptException, NoSuchMethodException {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.JS);
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 10);
        vars.put("b", 5);

        // expectation
        Object result = runner.evalWithTimeout("a - b", vars, 5000);

        // validation
        assertThat(((Number) result).intValue()).isEqualTo(5);
    }

    // ────────────────────────────────────────────────
    // String manipulation in scripts
    // ────────────────────────────────────────────────

    @Test
    void shouldHandleStringOperationsInJS() throws ScriptException, NoSuchMethodException {
        // setup
        ScriptRunner runner = new ScriptRunner(ScriptRunnerEngine.JS);
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "world");

        // expectation
        Object result = runner.eval("'hello ' + name", vars);

        // validation
        assertThat(result).isEqualTo("hello world");
    }
}
