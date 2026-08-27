package idv.mark.share_module.asr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ASRResult {
    private Long taskId;
    private String srtFilePath;
    private String rawOutput;
    private boolean success;
    private String errorMessage;
}
