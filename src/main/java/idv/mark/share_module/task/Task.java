package idv.mark.share_module.task;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID correlationId;

    @Column(nullable = false)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    private Integer progress;

    private Integer exitCode;

    @Column(length = 4000)
    private String command;

    @Column(length = 4000)
    private String errorMessage;

    @Column(length = 4000)
    private String metadata;

    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (correlationId == null) correlationId = UUID.randomUUID();
    }
}
