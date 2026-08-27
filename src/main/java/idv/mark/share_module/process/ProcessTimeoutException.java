package idv.mark.share_module.process;

public class ProcessTimeoutException extends RuntimeException {
    private final Long taskId;
    private final long timeoutSeconds;

    public ProcessTimeoutException(Long taskId, long timeoutSeconds) {
        super(String.format("Process for task %d timed out after %d seconds", taskId, timeoutSeconds));
        this.taskId = taskId;
        this.timeoutSeconds = timeoutSeconds;
    }

    public Long getTaskId() { return taskId; }
    public long getTimeoutSeconds() { return timeoutSeconds; }
}
