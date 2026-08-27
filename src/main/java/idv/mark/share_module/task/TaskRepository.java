package idv.mark.share_module.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByCorrelationId(UUID correlationId);

    List<Task> findByStatusIn(List<TaskStatus> statuses);

    @Query("SELECT t FROM Task t WHERE t.status IN :statuses ORDER BY t.createdAt DESC")
    List<Task> findActiveTasksOrderByCreatedAtDesc(@Param("statuses") List<TaskStatus> statuses);

    @Query(value = "SELECT * FROM tasks ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Task> findRecentTasks(@Param("limit") int limit);

    @Modifying
    @Transactional
    @Query("UPDATE Task t SET t.progress = :progress WHERE t.id = :id")
    void updateProgress(@Param("id") Long id, @Param("progress") Integer progress);

    @Modifying
    @Transactional
    @Query("DELETE FROM Task t WHERE t.status IN ('COMPLETED','FAILED','TIMEOUT','CANCELLED') AND t.completedAt < :before")
    int cleanupOldTasks(@Param("before") Instant before);
}
