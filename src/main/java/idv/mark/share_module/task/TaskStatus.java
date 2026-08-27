package idv.mark.share_module.task;

public enum TaskStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    TIMEOUT,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == TIMEOUT || this == CANCELLED;
    }

    public boolean isActive() {
        return this == QUEUED || this == RUNNING;
    }
}
