package idv.mark.share_module.asr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ASRRequest {
    private String audioFilePath;
    private String[] languages;
    private String modelName;
    private UUID correlationId;
    private String outputDir;
}
