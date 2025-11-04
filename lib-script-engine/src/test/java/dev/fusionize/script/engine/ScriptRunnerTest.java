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
}