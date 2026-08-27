package idv.mark.share_module.process;

import idv.mark.share_module.task.Task;
import idv.mark.share_module.task.TaskRepository;
import idv.mark.share_module.task.TaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
@Component
public class ProcessManager {

    @Autowired
    private TaskRepository taskRepository;

    private final ExecutorService ioExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("process-io-" + t.getId());
        return t;
    });

    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("process-async-" + t.getId());
        return t;
    });

    /**
     * 同步執行外部命令，帶完整生命週期管理
     */
    public ProcessResult execute(ProcessRequest request) {
        Task task = taskRepository.save(Task.builder()
            .correlationId(request.getCorrelationId())
            .type(request.getTaskType())
            .status(TaskStatus.RUNNING)
            .command(String.join(" ", request.getCommand()))
            .startedAt(Instant.now())
            .build());

        ProcessBuilder pb = new ProcessBuilder(request.getCommand());
        if (request.getWorkDir() != null) pb.directory(request.getWorkDir());
        if (request.getEnvVars() != null && !request.getEnvVars().isEmpty()) {
            pb.environment().putAll(request.getEnvVars());
        }
        pb.redirectErrorStream(false);

        Process process = null;
        try {
            process = pb.start();
            final Process p = process;

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Future<?> stdoutFuture = ioExecutor.submit(() ->
                readStream(p.getInputStream(), stdout, line -> {
                    log.debug("[stdout] {}", line);
                    parseProgress(task.getId(), line, request.getProgressParser());
                })
            );
            Future<?> stderrFuture = ioExecutor.submit(() ->
                readStream(p.getErrorStream(), stderr, line -> {
                    log.debug("[stderr] {}", line);
                    parseProgress(task.getId(), line, request.getProgressParser());
                })
            );

            long timeout = request.getTimeoutSeconds() > 0 ? request.getTimeoutSeconds() : 3600;
            boolean finished = p.waitFor(timeout, TimeUnit.SECONDS);

            if (!finished) {
                p.destroyForcibly();
                updateTaskFinished(task.getId(), TaskStatus.TIMEOUT, -1, "Process timed out after " + timeout + "s");
                throw new ProcessTimeoutException(task.getId(), timeout);
            }

            stdoutFuture.get(10, TimeUnit.SECONDS);
            stderrFuture.get(10, TimeUnit.SECONDS);

            int exitCode = p.exitValue();
            TaskStatus finalStatus = exitCode == 0 ? TaskStatus.COMPLETED : TaskStatus.FAILED;
            String errorMsg = exitCode != 0 ? truncate(stderr.toString(), 4000) : null;
            updateTaskFinished(task.getId(), finalStatus, exitCode, errorMsg);

            return new ProcessResult(task.getId(), exitCode, stdout.toString(), stderr.toString());

        } catch (ProcessTimeoutException e) {
            throw e;
        } catch (Exception e) {
            updateTaskFinished(task.getId(), TaskStatus.FAILED, -1, truncate(e.getMessage(), 4000));
            throw new ProcessExecutionException(task.getId(), e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                log.warn("Force-killed process for task {}", task.getId());
            }
        }
    }

    /**
     * 非同步執行，立即返回 taskId
     */
    public Long executeAsync(ProcessRequest request, Consumer<ProcessResult> callback) {
        Task task = taskRepository.save(Task.builder()
            .correlationId(request.getCorrelationId())
            .type(request.getTaskType())
            .status(TaskStatus.QUEUED)
            .command(String.join(" ", request.getCommand()))
            .build());

        asyncExecutor.submit(() -> {
            try {
                ProcessResult result = execute(request);
                if (callback != null) callback.accept(result);
            } catch (Exception e) {
                log.error("Async process failed for task {}", task.getId(), e);
            }
        });

        return task.getId();
    }

    public Long executeAsync(ProcessRequest request) {
        return executeAsync(request, null);
    }

    private void readStream(InputStream is, StringBuilder sb, Consumer<String> lineCallback) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
                lineCallback.accept(line);
            }
        } catch (IOException e) {
            log.warn("Stream read error", e);
        }
    }

    private void parseProgress(Long taskId, String line, ProgressParser parser) {
        if (parser != null) {
            try {
                Integer progress = parser.parse(line);
                if (progress != null) {
                    taskRepository.updateProgress(taskId, progress);
                }
            } catch (Exception e) {
                // 進度解析失敗不影響主流程
            }
        }
    }

    private void updateTaskFinished(Long id, TaskStatus status, int exitCode, String errorMsg) {
        taskRepository.findById(id).ifPresent(t -> {
            t.setStatus(status);
            t.setExitCode(exitCode);
            t.setErrorMessage(errorMsg);
            t.setCompletedAt(Instant.now());
            taskRepository.save(t);
        });
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
