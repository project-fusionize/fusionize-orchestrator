package dev.fusionize.workflow.component.runtime.interfaces;

import dev.fusionize.workflow.WorkflowLog;
import dev.fusionize.workflow.context.WorkflowContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentUpdateEmitterTest {

    @Test
    void testDefaultLoggingMethods() {
        TestEmitter emitter = new TestEmitter();

        emitter.logger().info("Info message {}", "test");
        emitter.logger().warn("Warn message {}", "test");
        emitter.logger().error("Error message {}", "test");
        emitter.logger().debug("Debug message {}", "test");

        List<LogEntry> logs = emitter.getLogs();
        assertEquals(4, logs.size());

        assertEquals("Info message test", logs.get(0).message);
        assertEquals(WorkflowLog.LogLevel.INFO, logs.get(0).level);

        assertEquals("Warn message test", logs.get(1).message);
        assertEquals(WorkflowLog.LogLevel.WARN, logs.get(1).level);

        assertEquals("Error message test", logs.get(2).message);
        assertEquals(WorkflowLog.LogLevel.ERROR, logs.get(2).level);

        assertEquals("Debug message test", logs.get(3).message);
        assertEquals(WorkflowLog.LogLevel.DEBUG, logs.get(3).level);
    }

    static class TestEmitter implements ComponentUpdateEmitter {
        private final List<LogEntry> logs = new ArrayList<>();

        @Override
        public void success(WorkflowContext updatedContext) {
        }

        @Override
        public void failure(Exception ex) {
        }

        @Override
        public Logger logger() {
            return (message, level, throwable) -> logs.add(new LogEntry(message, level));
        }

        public List<LogEntry> getLogs() {
            return logs;
        }
    }

    static class LogEntry {
        String message;
        WorkflowLog.LogLevel level;

        public LogEntry(String message, WorkflowLog.LogLevel level) {
            this.message = message;
            this.level = level;
        }
    }
}
