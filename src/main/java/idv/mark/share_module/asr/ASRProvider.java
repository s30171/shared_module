package idv.mark.share_module.asr;

public interface ASRProvider {
    String getName();
    ASRResult transcribe(ASRRequest request);
    boolean supportsLanguages(String[] languages);
}
