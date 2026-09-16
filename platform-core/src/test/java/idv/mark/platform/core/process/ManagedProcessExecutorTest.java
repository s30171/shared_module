package idv.mark.platform.core.process;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class ManagedProcessExecutorTest {
    @TempDir Path temp;

    private ProcessSpec spec(String id, String mode, Duration deadline) {
        return new ProcessSpec(id, List.of(ProcessFixture.java(), "-cp", System.getProperty("java.class.path"), ProcessFixture.class.getName(), mode),
                temp, Map.of(), deadline, Duration.ofMillis(100), temp.resolve("logs"), 4096, 8192, 2);
    }

    @Test void drainsBothStreamsAndBoundsMemoryAndDisk() throws Exception {
        try (ManagedProcessExecutor executor = new ManagedProcessExecutor()) {
            ProcessOutcome result = executor.execute(spec("flood", "flood", Duration.ofSeconds(30)), null, null);
            assertTrue(result.success(), result.error()); assertEquals("flood", result.executionId());
            assertTrue(result.stdoutTail().length() <= 4096); assertTrue(result.stderrTail().length() <= 4096);
            try (var files = Files.list(temp.resolve("logs"))) {
                List<Path> logs = files.toList(); assertTrue(logs.size() <= 4);
                for (Path log : logs) assertTrue(Files.size(log) <= 8192, log.toString());
            }
            assertEquals(0, executor.activeCount());
        }
    }

    @Test void timesOutSilentProcessAndReportsSameId() {
        try (ManagedProcessExecutor executor = new ManagedProcessExecutor()) {
            ProcessOutcome result = executor.execute(spec("silent", "silent", Duration.ofMillis(400)), null, null);
            assertEquals(ProcessOutcome.State.TIMEOUT, result.state());
            assertEquals("silent", result.executionId());
            assertFalse(ProcessHandle.of(result.pid()).map(ProcessHandle::isAlive).orElse(false));
        }
    }

    @Test void cancellationStopsObservedChildAndParent() throws Exception {
        AtomicLong childId = new AtomicLong();
        try (ManagedProcessExecutor executor = new ManagedProcessExecutor()) {
            CompletableFuture<ProcessOutcome> future = CompletableFuture.supplyAsync(() -> executor.execute(spec("tree", "child", Duration.ZERO), new ProcessObserver() {
                @Override public void output(String id, String stream, String line, Instant at) {
                    if (line.startsWith("CHILD=")) childId.set(Long.parseLong(line.substring(6)));
                }
            }, null));
            long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            while (childId.get() == 0 && System.nanoTime() < deadline) Thread.sleep(20);
            assertNotEquals(0, childId.get());
            assertTrue(executor.cancel("tree"));
            ProcessOutcome result = future.get(10, TimeUnit.SECONDS);
            assertEquals(ProcessOutcome.State.CANCELLED, result.state());
            assertFalse(ProcessHandle.of(childId.get()).map(ProcessHandle::isAlive).orElse(false));
            assertFalse(ProcessHandle.of(result.pid()).map(ProcessHandle::isAlive).orElse(false));
        }
    }

    @Test void preservesFailureAndRedactsLogs() throws Exception {
        try (ManagedProcessExecutor executor = new ManagedProcessExecutor(new SecretRedactor(List.of("super-secret-value")))) {
            ProcessOutcome result = executor.execute(spec("failure", "fail", Duration.ofSeconds(5)), null, null);
            assertEquals(ProcessOutcome.State.FAILED, result.state()); assertEquals(7, result.exitCode());
            assertFalse(result.stderrTail().contains("super-secret-value"));
            assertFalse(Files.readString(temp.resolve("logs/failure.stderr.log")).contains("super-secret-value"));
        }
    }

    @Test void boundsAnUnterminatedOutputLine() {
        try (ManagedProcessExecutor executor = new ManagedProcessExecutor()) {
            ProcessOutcome result = executor.execute(spec("giant", "giant", Duration.ofSeconds(10)), null, null);
            assertTrue(result.success(), result.error()); assertTrue(result.stdoutTail().length() <= 4096);
        }
    }

    @Test void cancellingBeforeStartDoesNotLaunchAProcess() {
        try (ManagedProcessExecutor executor = new ManagedProcessExecutor()) {
            ProcessOutcome result = executor.execute(spec("cancelled", "silent", Duration.ZERO), null, () -> true);
            assertEquals(ProcessOutcome.State.CANCELLED, result.state()); assertNull(result.pid());
        }
    }

    @Test void liveMergeDoesNotInventAPercentage() {
        assertNull(ProgressDecoder.decode("ytarchive", "Muxing final file", null).percent());
        assertEquals(50d, ProgressDecoder.decode("ffmpeg", "time=00:05:00.000", 600_000L).percent());
    }
}
