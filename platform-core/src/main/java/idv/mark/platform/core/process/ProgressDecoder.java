package idv.mark.platform.core.process;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProgressDecoder {
    private static final Pattern PERCENT = Pattern.compile("(?:\\[download\\]\\s*|^\\s*)(\\d+(?:\\.\\d+)?)%(?:\\||\\s)");
    private static final Pattern FFMPEG = Pattern.compile("time=(\\d+):(\\d{2}):(\\d{2}(?:\\.\\d+)?)");
    private ProgressDecoder() {}

    public static Progress decode(String tool, String line, Long durationMillis) {
        if ("ffmpeg".equals(tool)) {
            Matcher match = FFMPEG.matcher(line);
            if (match.find()) {
                long millis = Math.round((Long.parseLong(match.group(1)) * 3600 + Long.parseLong(match.group(2)) * 60 + Double.parseDouble(match.group(3))) * 1000);
                return new Progress("PROCESSING", durationMillis != null && durationMillis > 0 ? Math.min(100d, 100d * millis / durationMillis) : null, millis, null, null, null, null);
            }
        }
        if ("ytarchive".equals(tool)) {
            if (line.toLowerCase().contains("muxing")) return new Progress("MERGING", null, null, null, null, null, null);
            return null; // A live recording has no known total duration.
        }
        Matcher match = PERCENT.matcher(line);
        if (match.find()) return new Progress("PROCESSING", Math.max(0d, Math.min(100d, Double.parseDouble(match.group(1)))), null, null, null, null, null);
        return null;
    }
}
