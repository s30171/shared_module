package idv.mark.platform.core.process;

import idv.mark.platform.core.model.ArtifactPaths;
import idv.mark.platform.core.model.SubtitleFormats;
import idv.mark.platform.core.model.Transcript;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ModelContractTest {
    @TempDir Path root;
    @Test void supportsShortWebVttTimingsAndKoreanWordSpacing() {
        var parsed=SubtitleFormats.parse("\uFEFFWEBVTT\n\nNOTE test\n\n00:01.250 --> 00:02.500 align:start\nhello\n");assertEquals(1250,parsed.get(0).startMs());
        var words=List.of(new Transcript.Word(0,500,"안녕하세요",null,null),new Transcript.Word(500,900,"여러분",null,null));assertEquals("안녕하세요 여러분",SubtitleFormats.fromWords(words).get(0).text());
    }
    @Test void preservesOverlappingSpeakersAndMillisecondTiming() {
        List<Transcript.Segment> source = List.of(new Transcript.Segment(123, 1123, "first", "A"), new Transcript.Segment(500, 1500, "second", "B"));
        var parsed = SubtitleFormats.parse(SubtitleFormats.render(source, false));
        assertEquals(123, parsed.get(0).startMs()); assertEquals(1500, parsed.get(1).endMs());
        assertEquals("[A] first", parsed.get(0).text());
    }
    @Test void rejectsFakeOrEmptySubtitles() {
        assertThrows(IllegalArgumentException.class, () -> SubtitleFormats.parse("{\"result\":\"hello\"}"));
        assertThrows(IllegalArgumentException.class, () -> SubtitleFormats.parse("1\n00:00:02,000 --> 00:00:01,000\nhello"));
        assertThrows(IllegalArgumentException.class, () -> SubtitleFormats.render(List.of(), false));
    }
    @Test void boundsArtifactPaths() throws Exception {
        assertEquals(root.resolve("tasks/test/audio.mp3"), ArtifactPaths.resolve(root, "tasks/test/audio.mp3"));
        assertThrows(IllegalArgumentException.class, () -> ArtifactPaths.resolve(root, "../outside"));
        assertThrows(IllegalArgumentException.class, () -> ArtifactPaths.resolve(root, root.toAbsolutePath().toString()));
    }
}
