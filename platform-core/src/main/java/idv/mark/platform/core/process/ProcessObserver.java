package idv.mark.platform.core.process;

import java.time.Instant;

/** Callbacks must be fast: enqueue/coalesce network updates, never block a stream drainer. */
public interface ProcessObserver {
    ProcessObserver NONE = new ProcessObserver() {};
    default void started(String executionId, long pid, Instant processStartedAt) {}
    default void output(String executionId, String stream, String line, Instant observedAt) {}
    default void finished(ProcessOutcome outcome) {}
}
