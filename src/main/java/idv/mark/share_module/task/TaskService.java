package idv.mark.share_module.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    private final TaskRepository taskRepository;

    public Task createTask(UUID correlationId, String type) {
        return taskRepository.save(Task.builder()
            .correlationId(correlationId)
            .type(type)
            .status(TaskStatus.QUEUED)
            .build());
    }

    public List<Task> getActiveTasks() {
        return taskRepository.findActiveTasksOrderByCreatedAtDesc(
            List.of(TaskStatus.QUEUED, TaskStatus.RUNNING));
    }

    public List<Task> getTasksByCorrelation(UUID correlationId) {
        return taskRepository.findByCorrelationId(correlationId);
    }

    public List<Task> getRecentTasks(int limit) {
        return taskRepository.findRecentTasks(limit);
    }

    @Transactional
    public void updateStatus(Long id, TaskStatus status) {
        taskRepository.findById(id).ifPresent(t -> {
            t.setStatus(status);
            if (status == TaskStatus.RUNNING && t.getStartedAt() == null) {
                t.setStartedAt(Instant.now());
            }
            if (status.isTerminal()) {
                t.setCompletedAt(Instant.now());
            }
            taskRepository.save(t);
        });
    }

    @Transactional
    public void updateStatus(Long id, TaskStatus status, String errorMessage) {
        taskRepository.findById(id).ifPresent(t -> {
            t.setStatus(status);
            t.setErrorMessage(errorMessage);
            if (status.isTerminal()) {
                t.setCompletedAt(Instant.now());
            }
            taskRepository.save(t);
        });
    }

    @Transactional
    public void updateMetadata(Long id, String metadata) {
        taskRepository.findById(id).ifPresent(t -> {
            t.setMetadata(metadata);
            taskRepository.save(t);
        });
    }

    public Task save(Task task) {
        return taskRepository.save(task);
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        int deleted = taskRepository.cleanupOldTasks(Instant.now().minus(7, ChronoUnit.DAYS));
        if (deleted > 0) {
            log.info("Cleaned up {} old tasks", deleted);
        }
    }
}
