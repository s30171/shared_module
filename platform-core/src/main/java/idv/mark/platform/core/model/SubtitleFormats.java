package idv.mark.platform.core.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SubtitleFormats {
    private static final Pattern TIMING = Pattern.compile("(?m)^(?:(\\d{2,}):)?(\\d{2}):(\\d{2})[,.](\\d{3})\\s*-->\\s*(?:(\\d{2,}):)?(\\d{2}):(\\d{2})[,.](\\d{3})(?:[^\\r\\n]*)$");
    private SubtitleFormats() {}

    public static List<Transcript.Segment> parse(String input) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Empty subtitle");
        List<Transcript.Segment> result = new ArrayList<>();
        for (String block : input.replace("\uFEFF", "").replace("\r\n", "\n").replace('\r', '\n').strip().split("\n[ \\t]*\n")) {
            if (block.matches("(?s)^(WEBVTT|NOTE|STYLE|REGION)(\\s.*)?$")) continue;
            Matcher matcher = TIMING.matcher(block);
            if (!matcher.find()) throw new IllegalArgumentException("Missing or invalid subtitle timestamp");
            String text = block.substring(matcher.end()).strip();
            result.add(new Transcript.Segment(millis(matcher, 1), millis(matcher, 5), text, null));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Subtitle contains no segments");
        long last = -1;
        for (Transcript.Segment segment : result) {
            if (segment.startMs() < last) throw new IllegalArgumentException("Subtitle segments are out of order");
            last = segment.startMs();
        }
        return List.copyOf(result);
    }

    private static long millis(Matcher matcher, int offset) {
        long hours = matcher.group(offset)==null?0:Long.parseLong(matcher.group(offset));
        int minutes = Integer.parseInt(matcher.group(offset + 1)), seconds = Integer.parseInt(matcher.group(offset + 2));
        if (minutes > 59 || seconds > 59) throw new IllegalArgumentException("Invalid subtitle time");
        return ((hours * 60 + minutes) * 60 + seconds) * 1000 + Integer.parseInt(matcher.group(offset + 3));
    }

    public static String render(List<Transcript.Segment> segments, boolean vtt) {
        if (segments == null || segments.isEmpty()) throw new IllegalArgumentException("No timed segments available");
        StringBuilder output = new StringBuilder(vtt ? "WEBVTT\n\n" : "");
        int index = 1;
        for (Transcript.Segment segment : segments.stream().sorted(Comparator.comparingLong(Transcript.Segment::startMs)).toList()) {
            output.append(index++).append('\n').append(time(segment.startMs(), vtt)).append(" --> ").append(time(segment.endMs(), vtt)).append('\n');
            if (segment.speakerId() != null && !segment.speakerId().isBlank()) output.append('[').append(segment.speakerId().replaceAll("[\\r\\n]", " ")).append("] ");
            output.append(segment.text().strip().replaceAll("\\n\\s*\\n", "\n")).append("\n\n");
        }
        return output.toString();
    }

    private static String time(long millis, boolean vtt) {
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d%s%03d", millis / 3_600_000, millis / 60_000 % 60, millis / 1000 % 60, vtt ? "." : ",", millis % 1000);
    }

    /** Group real timed words; this never fabricates timing for an untimed transcript. */
    public static List<Transcript.Segment> fromWords(List<Transcript.Word> words) {
        List<Transcript.Segment> result = new ArrayList<>();
        StringBuilder text = new StringBuilder(); long start = 0, end = 0; String speaker = null;
        for (Transcript.Word word : words) {
            if (word.text().isBlank()) continue;
            boolean boundary = !text.isEmpty() && (!java.util.Objects.equals(speaker, word.speakerId()) || word.endMs() - start > 6000 || text.length() > 70);
            if (boundary) { if (end > start) result.add(new Transcript.Segment(start, end, text.toString().strip(), speaker)); text.setLength(0); }
            if (text.isEmpty()) { start = word.startMs(); speaker = word.speakerId(); }
            if (!text.isEmpty() && !endsInSpace(text) && needsWordSpace(word.text())) text.append(' ');
            text.append(word.text()); end = Math.max(start, word.endMs());
        }
        if (!text.isEmpty() && end > start) result.add(new Transcript.Segment(start, end, text.toString().strip(), speaker));
        return List.copyOf(result);
    }
    private static boolean endsInSpace(StringBuilder value) { return Character.isWhitespace(value.charAt(value.length() - 1)); }
    private static boolean needsWordSpace(String text) {
        int first=text.codePointAt(0);if(!Character.isLetterOrDigit(first))return false;
        return !java.util.Set.of(Character.UnicodeScript.HAN,Character.UnicodeScript.HIRAGANA,Character.UnicodeScript.KATAKANA,Character.UnicodeScript.THAI).contains(Character.UnicodeScript.of(first));
    }
}
