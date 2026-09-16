package idv.mark.platform.core.process;

import java.time.Instant;

public record ProcessOutcome(String executionId, State state, Long pid, Integer exitCode,
                             Instant startedAt, Instant completedAt, String stdoutTail,
                             String stderrTail, String error) {
    public enum State { COMPLETED, FAILED, TIMEOUT, CANCELLED }
    public boolean success() { return state == State.COMPLETED; }
}
