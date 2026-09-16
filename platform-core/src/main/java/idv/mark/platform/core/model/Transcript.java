package idv.mark.platform.core.model;

import java.util.List;
import java.util.Map;

public record Transcript(String provider, String model, String language, String text,
                         List<Segment> segments, List<Word> words, Map<String, Object> usage) {
    public Transcript {
        text = text == null ? "" : text;
        segments = segments == null ? List.of() : List.copyOf(segments);
        words = words == null ? List.of() : List.copyOf(words);
        usage = usage == null ? Map.of() : Map.copyOf(usage);
    }
    public record Segment(long startMs, long endMs, String text, String speakerId) {
        public Segment {
            if (startMs < 0 || endMs <= startMs || text == null || text.isBlank()) throw new IllegalArgumentException("Invalid subtitle segment");
        }
    }
    public record Word(long startMs, long endMs, String text, String speakerId, Double confidence) {
        public Word {
            if (startMs < 0 || endMs < startMs || text == null) throw new IllegalArgumentException("Invalid transcript word");
        }
    }
}
