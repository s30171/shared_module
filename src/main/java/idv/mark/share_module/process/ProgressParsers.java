package idv.mark.share_module.process;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProgressParsers {
    private ProgressParsers() {}

    /** yt-dlp: [download]  45.2% of 1.23GiB */
    public static final ProgressParser YT_DLP = line -> {
        Matcher m = Pattern.compile("\\[download\\]\\s+(\\d+\\.?\\d*)%").matcher(line);
        return m.find() ? (int) Double.parseDouble(m.group(1)) : null;
    };

    /** ytarchive: 會輸出 Fragments: X 之類的資訊，不好算精確百分比 */
    public static final ProgressParser YTARCHIVE = line -> {
        if (line.contains("Muxing final file")) return 95;
        if (line.contains("Finished")) return 100;
        return null;
    };

    /** FFmpeg: time=00:15:30.50 (需搭配總時長) */
    public static ProgressParser ffmpeg(Duration totalDuration) {
        return line -> {
            Matcher m = Pattern.compile("time=(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{2})").matcher(line);
            if (!m.find() || totalDuration.isZero()) return null;
            long h = Long.parseLong(m.group(1));
            long min = Long.parseLong(m.group(2));
            long s = Long.parseLong(m.group(3));
            long cs = Long.parseLong(m.group(4));
            long currentMs = (h * 3600 + min * 60 + s) * 1000 + cs * 10;
            return (int) Math.min(100, currentMs * 100 / totalDuration.toMillis());
        };
    }

    /** Whisper tqdm progress bar: 45%|████     | */
    public static final ProgressParser WHISPER = line -> {
        Matcher m = Pattern.compile("(\\d+)%\\|").matcher(line);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    };
}
