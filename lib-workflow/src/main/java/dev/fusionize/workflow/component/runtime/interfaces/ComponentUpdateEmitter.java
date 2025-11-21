package dev.fusionize.workflow.component.runtime.interfaces;

import dev.fusionize.workflow.WorkflowLog;
import dev.fusionize.workflow.context.WorkflowContext;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public interface ComponentUpdateEmitter {
    void success(WorkflowContext updatedContext);

    void failure(Exception ex);

    Logger logger();

    interface Logger {

        /**
         * Core logging method with optional Throwable
         */
        void log(String message, WorkflowLog.LogLevel level, Throwable throwable);

        /**
         * Convenience log method without Throwable
         */
        default void log(String message, WorkflowLog.LogLevel level) {
            log(message, level, null);
        }

        // ---------------------------
        // Level-specific logging
        // ---------------------------
        default void info(String message, Object... args) {
            logWithThrowable(message, WorkflowLog.LogLevel.INFO, args);
        }

        default void warn(String message, Object... args) {
            logWithThrowable(message, WorkflowLog.LogLevel.WARN, args);
        }

        default void error(String message, Object... args) {
            logWithThrowable(message, WorkflowLog.LogLevel.ERROR, args);
        }

        default void debug(String message, Object... args) {
            logWithThrowable(message, WorkflowLog.LogLevel.DEBUG, args);
        }

        // ---------------------------
        // Internal helper: handle SLF4J formatting + Throwable
        // ---------------------------
        private void logWithThrowable(String message, WorkflowLog.LogLevel level, Object... args) {
            if (message == null) {
                log(null, level, null);
                return;
            }
            if (args == null || args.length == 0) {
                log(message, level, null);
                return;
            }

            FormattingTuple ft = MessageFormatter.arrayFormat(message, args);
            log(ft.getMessage(), level, ft.getThrowable());
        }
    }
}
