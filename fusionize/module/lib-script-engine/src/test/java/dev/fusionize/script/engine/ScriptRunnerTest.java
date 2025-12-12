package dev.fusionize.script.engine;

import org.junit.jupiter.api.Test;

import javax.script.ScriptException;

import static org.junit.jupiter.api.Assertions.*;

class ScriptRunnerTest {

    @Test
    void executeFunctionJS() throws ScriptException, NoSuchMethodException {
        ScriptRunner scriptRunner = new ScriptRunner(ScriptRunnerEngine.JS);
        String script = "function add(a,b){ return a+b; }";
        Object result = scriptRunner.executeFunction(script, "add", 10, 20);
        assertEquals(30, result);
    }

    @Test
    void executeFunctionGroovy() throws ScriptException, NoSuchMethodException {
        ScriptRunner scriptRunner = new ScriptRunner(ScriptRunnerEngine.GROOVY);
        String script = "def add(a,b){ return a+b; }";
        Object result = scriptRunner.executeFunction(script, "add", 10, 20);
        assertEquals(30, result);
    }

    @Test
    void executeFunctionKotlin() throws ScriptException, NoSuchMethodException {
        ScriptRunner scriptRunner = new ScriptRunner(ScriptRunnerEngine.KOTLIN);
        String script = "fun add(a: Int, b: Int): Int { return a + b; }";
        Object result = scriptRunner.executeFunction(script, "add", 10, 20);
        assertEquals(30, result);
    }

    @Test
    void testEvalJS() throws ScriptException, NoSuchMethodException {
        ScriptRunner scriptRunner = new ScriptRunner(ScriptRunnerEngine.JS);
        Object result = scriptRunner.eval("10 + 20", null);
        assertEquals(30, result);
    }

    @Test
    void testEvalGroovy() throws ScriptException, NoSuchMethodException {
        ScriptRunner scriptRunner = new ScriptRunner(ScriptRunnerEngine.GROOVY);
        Object result = scriptRunner.eval("10 + 20", null);
        assertEquals(30, result);
    }

    @Test
    void testEvalKotlin() throws ScriptException, NoSuchMethodException {
        ScriptRunner scriptRunner = new ScriptRunner(ScriptRunnerEngine.KOTLIN);
        Object result = scriptRunner.eval("10 + 20", null);
        assertEquals(30, result);
    }

    @Test
    void testEvalWithVariables() throws ScriptException, NoSuchMethodException {
        ScriptRunner scriptRunner = new ScriptRunner(ScriptRunnerEngine.JS);
        java.util.Map<String, Object> vars = new java.util.HashMap<>();
        vars.put("a", 10);
        vars.put("b", 20);
        Object result = scriptRunner.eval("a + b", vars);
        assertEquals(30.0, ((Number) result).doubleValue());
    }

    @Test
    void testEvalTimeout() {
        ScriptRunner scriptRunner = new ScriptRunner(ScriptRunnerEngine.JS);
        assertThrows(ScriptException.class, () -> {
            scriptRunner.evalWithTimeout("while(true){}", null, 100);
        });
    }

    @Test
    void testSecurity() {
        ScriptRunner scriptRunner = new ScriptRunner(ScriptRunnerEngine.JS);
        // Attempt to access java.io.File should fail due to RestrictedClassLoader
        assertThrows(ScriptException.class, () -> {
            scriptRunner.eval("var File = Java.type('java.io.File'); new File('/tmp/test');", null);
        });
    }
}