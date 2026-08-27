package idv.mark.share_module.asr;

import idv.mark.share_module.process.ProcessManager;
import idv.mark.share_module.process.ProcessRequest;
import idv.mark.share_module.process.ProcessResult;
import idv.mark.share_module.process.ProgressParsers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WhisperXProvider implements ASRProvider {
    private final ProcessManager processManager;

    @Override
    public String getName() {
        return "WHISPER_X";
    }

    @Override
    public boolean supportsLanguages(String[] languages) {
        return languages == null || languages.length <= 1;
    }

    @Override
    public ASRResult transcribe(ASRRequest request) {
        String lang = (request.getLanguages() != null && request.getLanguages().length > 0)
            ? request.getLanguages()[0] : "ko";

        List<String> cmd = new ArrayList<>(List.of(
            "whisperx", request.getAudioFilePath(),
            "--model", StringUtils.defaultIfBlank(request.getModelName(), "large"),
            "--language", lang,
            "--output_format", "srt",
            "--output_dir", request.getOutputDir(),
            "--chunk_size", Set.of("zh", "ja").contains(lang) ? "6" : "8"
        ));
        if ("th".equals(lang)) {
            cmd.add("--no_align");
        }

        ProcessResult result = processManager.execute(ProcessRequest.builder()
            .command(cmd)
            .taskType("ASR_WHISPER_X")
            .timeoutSeconds(7200)
            .correlationId(request.getCorrelationId())
            .progressParser(ProgressParsers.WHISPER)
            .build());

        return ASRResult.builder()
            .taskId(result.getTaskId())
            .srtFilePath(findSrtInDir(request.getOutputDir()))
            .success(result.isSuccess())
            .errorMessage(result.isSuccess() ? null : result.getStderr())
            .build();
    }

    private String findSrtInDir(String dir) {
        File directory = new File(dir);
        if (directory.exists() && directory.isDirectory()) {
            File[] srtFiles = directory.listFiles((d, name) -> name.endsWith(".srt"));
            if (srtFiles != null && srtFiles.length > 0) {
                return srtFiles[0].getAbsolutePath();
            }
        }
        return null;
    }
}
