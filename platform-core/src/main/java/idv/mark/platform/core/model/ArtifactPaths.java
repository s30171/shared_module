package idv.mark.platform.core.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ArtifactPaths {
    private ArtifactPaths() {}
    public static Path resolve(Path root, String key) throws IOException {
        if (key == null || key.isBlank() || key.contains("\\") || key.indexOf('\0') >= 0) throw new IllegalArgumentException("Invalid artifact key");
        Path relative = Path.of(key);
        if (relative.isAbsolute()) throw new IllegalArgumentException("Artifact key must be relative");
        Path base = root.toAbsolutePath().normalize();
        Path target = base.resolve(relative).normalize();
        if (target.equals(base) || !target.startsWith(base)) throw new IllegalArgumentException("Artifact outside storage root");
        Path current = base;
        if (Files.isSymbolicLink(base)) throw new IllegalArgumentException("Storage root cannot be a symlink");
        for (Path component : base.relativize(target)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) throw new IllegalArgumentException("Artifact path cannot contain symlinks");
        }
        return target;
    }
}
