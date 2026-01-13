package org.codingmatters.tasks.context;

import org.codingmatters.poom.services.logging.CategorizedLogger;

public class TaskContext {

    static public TaskLogger log() {
        return PER_THREAD_INSTANCE.get();
    }

    static public void setupLog(TaskLogger logger) {
        PER_THREAD_INSTANCE.set(logger != null ? logger : FALLBACK);
    }

    static private final CategorizedLogger logger = CategorizedLogger.getLogger(TaskContext.class);

    static private TaskLogger FALLBACK = new TaskLogger() {
        @Override
        public void info(String log, Object... args) {
            logger.info(String.format(log, args));
        }

        @Override
        public void warn(String log, Object... args) {
            logger.warn(String.format(log, args));
        }

        @Override
        public void error(String log, Object... args) {
            logger.error(String.format(log, args));
        }
    };

    static private ThreadLocal<TaskLogger> PER_THREAD_INSTANCE = ThreadLocal.withInitial(() -> FALLBACK);
}
