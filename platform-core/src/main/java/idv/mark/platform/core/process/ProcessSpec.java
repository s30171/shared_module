package idv.mark.platform.core.process;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable request. A zero timeout explicitly means no overall deadline (live recording). */
public record ProcessSpec(String executionId, List<String> command, Path directory,
                          Map<String, String> environment, Duration timeout,
                          Duration terminationGrace, Path logDirectory,
                          int tailCharacters, long logFileBytes, int logFiles) {
    public ProcessSpec {
        Objects.requireNonNull(executionId, "executionId");
        if (!executionId.matches("[a-zA-Z0-9_-]{1,100}")) throw new IllegalArgumentException("Invalid execution ID");
        command = List.copyOf(command);
        if (command.isEmpty() || command.get(0).isBlank()) throw new IllegalArgumentException("Empty command");
        environment = environment == null ? Map.of() : Map.copyOf(environment);
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(terminationGrace, "terminationGrace");
        if (timeout.isNegative() || terminationGrace.isNegative()) throw new IllegalArgumentException("Negative timeout");
        if (tailCharacters < 1 || logFileBytes < 1024 || logFiles < 1 || logFiles > 20) throw new IllegalArgumentException("Invalid output limits");
    }

    public static ProcessSpec of(String id, List<String> command, Path directory, Path logs) {
        return new ProcessSpec(id, command, directory, Map.of(), Duration.ofHours(1),
                Duration.ofSeconds(5), logs, 32_768, 5 * 1024 * 1024, 3);
    }
}
