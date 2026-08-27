package idv.mark.share_module.asr;

import idv.mark.share_module.process.ProcessManager;
import idv.mark.share_module.process.ProcessRequest;
import idv.mark.share_module.process.ProcessResult;
import idv.mark.share_module.process.ProgressParsers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsanelyFastWhisperProvider implements ASRProvider {
    private final ProcessManager processManager;

    @Override
    public String getName() {
        return "INSANELY_FAST_WHISPER";
    }

    @Override
    public boolean supportsLanguages(String[] languages) {
        return languages != null && languages.length > 1;
    }

    @Override
    public ASRResult transcribe(ASRRequest request) {
        String outputJson = request.getOutputDir() + "/output.json";

        ProcessResult result = processManager.execute(ProcessRequest.builder()
            .command(List.of(
                "insanely-fast-whisper",
                "--file-name", request.getAudioFilePath(),
                "--model-name", StringUtils.defaultIfBlank(request.getModelName(), "openai/whisper-large-v3"),
                "--task", "transcribe",
                "--batch-size", "12",
                "--output-path", outputJson
            ))
            .taskType("ASR_INSANELY_FAST_WHISPER")
            .timeoutSeconds(7200)
            .correlationId(request.getCorrelationId())
            .progressParser(ProgressParsers.WHISPER)
            .build());

        // TODO: 遷移現有 InsanelyWhisperOutput JSON → SRT 轉換邏輯到這裡
        // 目前先回傳 JSON 路徑，讓上層服務處理轉換
        return ASRResult.builder()
            .taskId(result.getTaskId())
            .srtFilePath(outputJson)
            .rawOutput(result.getStdout())
            .success(result.isSuccess())
            .errorMessage(result.isSuccess() ? null : result.getStderr())
            .build();
    }
}
