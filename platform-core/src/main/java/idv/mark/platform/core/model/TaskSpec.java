package idv.mark.platform.core.model;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record TaskSpec(String workflow, Source source, Asr asr, Translation translation, Output output) {
    private static final Set<String> WORKFLOWS = Set.of("CAPTURE_ONLY", "BACKUP_AND_SUBTITLES", "TRANSCRIBE", "TRANSLATE_SUBTITLES");
    public TaskSpec {
        workflow = workflow == null ? "TRANSCRIBE" : workflow;
        asr = asr == null ? new Asr(null, null, null, null, null) : asr;
        translation = translation == null ? new Translation(null, null) : translation;
        output = output == null ? new Output(false, false, null, null) : output;
    }
    public void validate() {
        if (!WORKFLOWS.contains(workflow)) throw new IllegalArgumentException("Unsupported workflow");
        if (source == null) throw new IllegalArgumentException("Source is required");
        if (source.kind()==null || !Set.of("LIVE_URL", "MEDIA_URL", "ARTIFACT").contains(source.kind())) throw new IllegalArgumentException("Unsupported source kind");
        if ("ARTIFACT".equals(source.kind())) {
            if (source.artifactId() == null || source.artifactId().isBlank()) throw new IllegalArgumentException("artifactId is required");
        } else {
            URI uri;
            try { uri = URI.create(source.url()); } catch (Exception ex) { throw new IllegalArgumentException("Invalid media URL"); }
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IllegalArgumentException("Media URL must be HTTP(S) without embedded credentials");
        }
        if (translation.targetLanguages().size() > 20) throw new IllegalArgumentException("Too many target languages");
        if (translation.targetLanguages().stream().anyMatch(s -> !s.matches("[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*"))) throw new IllegalArgumentException("Invalid target language");
        if (workflow.equals("TRANSLATE_SUBTITLES") && (!source.kind().equals("ARTIFACT") || output.uploadVideo())) throw new IllegalArgumentException("Subtitle translation requires an uploaded subtitle artifact and cannot upload video");
        if (workflow.equals("CAPTURE_ONLY") && (output.uploadVideo() || output.uploadSubtitles() || !translation.targetLanguages().isEmpty())) throw new IllegalArgumentException("Select a subtitle workflow to translate or upload results");
        if ((output.uploadVideo() || output.uploadSubtitles()) && (output.accountRef() == null || !output.accountRef().matches("[A-Za-z0-9_]{1,60}"))) throw new IllegalArgumentException("Select a configured YouTube account reference");
    }
    public record Source(String kind, String site, String url, String artifactId, String name) {}
    public record Asr(String provider, String model, Map<String, Object> options, Map<String, Object> providerOptions, String credentialRef) {
        public Asr {
            provider = provider == null ? "WHISPER_X" : provider;
            options = options == null ? Map.of() : Map.copyOf(options);
            providerOptions = providerOptions == null ? Map.of() : Map.copyOf(providerOptions);
        }
    }
    public record Translation(List<String> targetLanguages, String profileId) {
        public Translation { targetLanguages = targetLanguages == null ? List.of() : List.copyOf(targetLanguages); }
    }
    public record Output(boolean uploadVideo, boolean uploadSubtitles, String accountRef, String videoId) {}
}
