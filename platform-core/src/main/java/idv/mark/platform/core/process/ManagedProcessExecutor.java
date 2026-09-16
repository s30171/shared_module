package idv.mark.platform.core.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** No Spring/DB dependency: callers own durable execution IDs and capacity limits. */
public final class ManagedProcessExecutor implements AutoCloseable {
    private final ExecutorService streams = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "managed-process-stream"); thread.setDaemon(true); return thread;
    });
    private final ConcurrentMap<String, Running> active = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final SecretRedactor redactor;

    public ManagedProcessExecutor() { this(new SecretRedactor(List.of())); }
    public ManagedProcessExecutor(SecretRedactor redactor) { this.redactor = redactor; }

    public ProcessOutcome execute(ProcessSpec spec, ProcessObserver observer, BooleanSupplier cancelled) {
        if (closed.get()) throw new IllegalStateException("Executor is closed");
        Running running = new Running();
        if (active.putIfAbsent(spec.executionId(), running) != null) throw new IllegalArgumentException("Execution already active");
        observer = observer == null ? ProcessObserver.NONE : observer;
        BooleanSupplier cancellation = cancelled == null ? () -> false : cancelled;
        Instant started = Instant.now();
        BoundedOutput out = null, err = null;
        Future<?> outReader = null, errReader = null;
        ProcessOutcome.State state = ProcessOutcome.State.FAILED;
        Integer exitCode = null;
        String failure = null;
        boolean interrupted = false;
        AtomicReference<Throwable> streamError = new AtomicReference<>();
        try {
            out = new BoundedOutput(spec, "stdout"); err = new BoundedOutput(spec, "stderr");
            if (running.cancelled.get() || cancellation.getAsBoolean() || closed.get()) {
                state = ProcessOutcome.State.CANCELLED;
            } else {
                ProcessBuilder builder = new ProcessBuilder(spec.command());
                if (spec.directory() != null) builder.directory(spec.directory().toFile());
                builder.environment().putAll(spec.environment());
                Process process = builder.start(); running.process = process;
                process.getOutputStream().close();
                running.rememberDescendants();
                observer.started(spec.executionId(), process.pid(), process.info().startInstant().orElse(started));
                BoundedOutput finalOut = out, finalErr = err;
                ProcessObserver sink = observer;
                outReader = streams.submit(() -> drain(process.getInputStream(), finalOut, "stdout", spec.executionId(), sink, streamError));
                errReader = streams.submit(() -> drain(process.getErrorStream(), finalErr, "stderr", spec.executionId(), sink, streamError));
                long startTick = System.nanoTime();
                boolean ended;
                while (!(ended = process.waitFor(100, TimeUnit.MILLISECONDS))) {
                    running.rememberDescendants();
                    if (running.cancelled.get() || cancellation.getAsBoolean() || closed.get()) {
                        state = ProcessOutcome.State.CANCELLED; break;
                    }
                    if (!spec.timeout().isZero() && System.nanoTime() - startTick >= spec.timeout().toNanos()) {
                        state = ProcessOutcome.State.TIMEOUT; failure = "Process exceeded configured deadline"; break;
                    }
                    if (streamError.get() != null) throw new IOException("Process output could not be read or persisted", streamError.get());
                }
                if (ended) {
                    exitCode = process.exitValue();
                    state = running.cancelled.get() || cancellation.getAsBoolean() ? ProcessOutcome.State.CANCELLED
                            : exitCode == 0 ? ProcessOutcome.State.COMPLETED : ProcessOutcome.State.FAILED;
                }
                running.terminate(spec.terminationGrace());
                if (!process.isAlive()) exitCode = process.exitValue();
                awaitReader(outReader); awaitReader(errReader);
                if (streamError.get() != null) throw new IOException("Process output could not be read or persisted", streamError.get());
            }
        } catch (InterruptedException ex) {
            interrupted = true; state = ProcessOutcome.State.CANCELLED; failure = "Execution interrupted";
        } catch (Exception ex) {
            state = ProcessOutcome.State.FAILED;
            failure = redactor.redact(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        } finally {
            running.terminate(spec.terminationGrace());
            if (running.process != null) {
                close(running.process.getInputStream()); close(running.process.getErrorStream());
                if (!running.process.isAlive()) exitCode = running.process.exitValue();
            }
            if (outReader != null && !outReader.isDone()) outReader.cancel(true);
            if (errReader != null && !errReader.isDone()) errReader.cancel(true);
            active.remove(spec.executionId(), running);
        }
        ProcessOutcome result = new ProcessOutcome(spec.executionId(), state,
                running.process == null ? null : running.process.pid(), exitCode, started, Instant.now(),
                out == null ? "" : out.tail(), err == null ? "" : err.tail(), failure);
        try { observer.finished(result); }
        finally { if (interrupted) Thread.currentThread().interrupt(); }
        return result;
    }

    public boolean cancel(String executionId) {
        Running running = active.get(executionId);
        if (running == null) return false;
        running.cancelled.set(true); return true;
    }

    public int activeCount() { return active.size(); }

    private void drain(InputStream input, BoundedOutput output, String stream, String id,
                       ProcessObserver observer, AtomicReference<Throwable> error) {
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            StringBuilder line = new StringBuilder();
            char[] buffer = new char[2048]; int count;
            boolean oversized = false;
            while ((count = reader.read(buffer)) != -1) {
                for (int i = 0; i < count; i++) {
                    char value = buffer[i];
                    if (value == '\r' || value == '\n') {
                        if (oversized) {
                            line.append("[overlong output line omitted]"); oversized = false;
                        }
                        if (!line.isEmpty()) emit(output, observer, id, stream, line);
                    } else {
                        if (!oversized) {
                            line.append(value);
                            if (line.length() >= 16_384) { line.setLength(0); oversized = true; }
                        }
                    }
                }
            }
            if (oversized) line.append("[overlong output line omitted]");
            if (!line.isEmpty()) emit(output, observer, id, stream, line);
        } catch (Exception ex) { error.compareAndSet(null, ex); }
    }

    private void emit(BoundedOutput output, ProcessObserver observer, String id, String stream, StringBuilder line) throws IOException {
        String text = redactor.redact(line.toString()); line.setLength(0);
        output.append(text); observer.output(id, stream, text, Instant.now());
    }

    private static void awaitReader(Future<?> reader) throws Exception { reader.get(5, TimeUnit.SECONDS); }
    private static void close(InputStream stream) { try { stream.close(); } catch (IOException ignored) { /* Already closing. */ } }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        for (Running running : active.values()) { running.cancelled.set(true); running.terminate(Duration.ofSeconds(2)); }
        streams.shutdownNow();
    }

    private static final class Running {
        final AtomicBoolean cancelled = new AtomicBoolean();
        final Map<Long, ProcessHandle> descendants = new ConcurrentHashMap<>();
        volatile Process process;

        void rememberDescendants() {
            if (process != null) process.descendants().forEach(p -> descendants.put(p.pid(), p));
        }

        synchronized void terminate(Duration grace) {
            if (process == null) return;
            rememberDescendants();
            // Keep observed handles; descendants can be reparented when the root exits.
            descendants.values().stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroy);
            if (process.isAlive()) process.destroy();
            long deadline = System.nanoTime() + grace.toNanos();
            boolean interrupted = false;
            while (anyAlive() && System.nanoTime() < deadline) {
                try { Thread.sleep(20); } catch (InterruptedException ex) { interrupted = true; break; }
            }
            descendants.values().stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
            if (process.isAlive()) process.destroyForcibly();
            try { process.waitFor(2, TimeUnit.SECONDS); } catch (InterruptedException ex) { interrupted = true; }
            if (interrupted) Thread.currentThread().interrupt();
        }

        private boolean anyAlive() { return process.isAlive() || descendants.values().stream().anyMatch(ProcessHandle::isAlive); }
    }
}
