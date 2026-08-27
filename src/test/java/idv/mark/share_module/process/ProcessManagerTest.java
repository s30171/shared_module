package idv.mark.share_module.process;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessManagerTest {

    @Test
    void testYtDlpProgressParser() {
        String sampleLine1 = "[download]  45.2% of 1.23GiB at 10.5MiB/s ETA 01:23";
        Integer progress = ProgressParsers.YT_DLP.parse(sampleLine1);
        assertEquals(45, progress);

        String sampleLine2 = "[download] 100% of 500.00MiB in 00:30";
        assertEquals(100, ProgressParsers.YT_DLP.parse(sampleLine2));

        String nonProgressLine = "[info] Downloading webpage";
        assertNull(ProgressParsers.YT_DLP.parse(nonProgressLine));
    }

    @Test
    void testWhisperProgressParser() {
        String tqdmLine = "  50%|█████     | 50/100 [00:30<00:30,  1.65it/s]";
        Integer progress = ProgressParsers.WHISPER.parse(tqdmLine);
        assertEquals(50, progress);

        String nonProgress = "Detecting language using up to the first 30 seconds...";
        assertNull(ProgressParsers.WHISPER.parse(nonProgress));
    }

    @Test
    void testFFmpegProgressParser() {
        Duration totalDuration = Duration.ofMinutes(10); // 600s
        ProgressParser parser = ProgressParsers.ffmpeg(totalDuration);

        String ffmpegLine = "frame= 1200 fps= 60 q=28.0 size=   10240kB time=00:05:00.00 bitrate= 279.6kbits/s speed=2.1x";
        Integer progress = parser.parse(ffmpegLine);
        assertEquals(50, progress); // 5 mins / 10 mins = 50%
    }
}
