package idv.mark.share_module.process;

public class ProcessExecutionException extends RuntimeException {
    private final Long taskId;

    public ProcessExecutionException(Long taskId, Throwable cause) {
        super(String.format("Process execution failed for task %d: %s", taskId, cause.getMessage()), cause);
        this.taskId = taskId;
    }

    public Long getTaskId() { return taskId; }
}
