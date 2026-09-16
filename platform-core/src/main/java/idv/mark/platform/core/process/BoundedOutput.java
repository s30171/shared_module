package idv.mark.platform.core.process;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** A bounded in-memory tail and bounded, rotating UTF-8 files. One writer per stream. */
final class BoundedOutput {
    private final int maxCharacters;
    private final Path file;
    private final long maxBytes;
    private final int generations;
    private final StringBuilder tail = new StringBuilder();
    private long bytes;

    BoundedOutput(ProcessSpec spec, String stream) throws IOException {
        maxCharacters = spec.tailCharacters(); maxBytes = spec.logFileBytes(); generations = spec.logFiles();
        if (spec.logDirectory() == null) { file = null; return; }
        Path root = spec.logDirectory().toAbsolutePath().normalize();
        Files.createDirectories(root);
        file = root.resolve(spec.executionId() + "." + stream + ".log");
        bytes = Files.exists(file) ? Files.size(file) : 0;
    }

    synchronized void append(String line) throws IOException {
        String bounded = line.length() > maxCharacters ? line.substring(line.length() - maxCharacters) : line;
        tail.append(bounded).append('\n');
        if (tail.length() > maxCharacters) tail.delete(0, tail.length() - maxCharacters);
        if (file == null) return;
        byte[] data = (bounded + "\n").getBytes(StandardCharsets.UTF_8);
        if (data.length > maxBytes) data = "[oversized log record omitted]\n".getBytes(StandardCharsets.UTF_8);
        if (bytes + data.length > maxBytes) rotate();
        Files.write(file, data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        bytes += data.length;
    }

    private void rotate() throws IOException {
        for (int index = generations - 1; index >= 1; index--) {
            Path target = file.resolveSibling(file.getFileName() + "." + index);
            Path source = index == 1 ? file : file.resolveSibling(file.getFileName() + "." + (index - 1));
            Files.deleteIfExists(target);
            if (Files.exists(source)) Files.move(source, target);
        }
        if (generations == 1) Files.deleteIfExists(file);
        bytes = 0;
    }

    synchronized String tail() { return tail.toString(); }
}
