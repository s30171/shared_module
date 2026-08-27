package idv.mark.share_module.process;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRequest {
    private List<String> command;
    private File workDir;
    @Builder.Default
    private Map<String, String> envVars = Map.of();
    private String taskType;
    @Builder.Default
    private long timeoutSeconds = 3600;
    private ProgressParser progressParser;
    private UUID correlationId;
}
