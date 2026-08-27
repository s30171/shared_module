package idv.mark.share_module.asr;

import idv.mark.share_module.task.TaskService;
import idv.mark.share_module.task.Task;
import idv.mark.share_module.task.TaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Gemini 3.5 Transcribe Provider
 * 使用 Google Gemini API Interactions endpoint 進行語音轉文字
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "asr.gemini.enabled", havingValue = "true")
public class GeminiTranscribeProvider implements ASRProvider {

    @Value("${asr.gemini.api-key}")
    private String apiKey;

    @Value("${asr.gemini.model:gemini-3.5-transcribe}")
    private String modelName;

    @Autowired
    private TaskService taskService;

    private final Gson gson = new Gson();
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta";

    @Override
    public String getName() {
        return "GEMINI_TRANSCRIBE";
    }

    @Override
    public boolean supportsLanguages(String[] languages) {
        return true; // Gemini 3.5 Transcribe 支援 85+ 語言，自動偵測
    }

    @Override
    public ASRResult transcribe(ASRRequest request) {
        Task task = taskService.createTask(request.getCorrelationId(), "ASR_GEMINI_TRANSCRIBE");
        taskService.updateStatus(task.getId(), TaskStatus.RUNNING);

        try {
            // 1. 上傳音訊檔案到 Gemini Files API
            String fileUri = uploadFile(request.getAudioFilePath());
            log.info("Uploaded audio to Gemini Files API: {}", fileUri);

            // 2. 構建語言提示
            String languageHint = "";
            if (request.getLanguages() != null && request.getLanguages().length > 0) {
                languageHint = "Primary language: " + String.join(", ", request.getLanguages()) + ". ";
            }

            // 3. 呼叫 Interactions API 進行轉錄
            String transcription = callInteractionsAPI(fileUri, languageHint, detectMimeType(request.getAudioFilePath()));
            log.info("Gemini transcription completed, output length: {}", transcription.length());

            // 4. 寫入 SRT 檔案
            String srtPath = request.getOutputDir() + "/gemini_output.srt";
            Files.createDirectories(Path.of(request.getOutputDir()));
            Files.writeString(Path.of(srtPath), transcription);

            taskService.updateStatus(task.getId(), TaskStatus.COMPLETED);

            return ASRResult.builder()
                .taskId(task.getId())
                .srtFilePath(srtPath)
                .rawOutput(transcription)
                .success(true)
                .build();

        } catch (Exception e) {
            log.error("Gemini Transcribe failed", e);
            taskService.updateStatus(task.getId(), TaskStatus.FAILED, e.getMessage());
            return ASRResult.builder()
                .taskId(task.getId())
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    private String uploadFile(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String mimeType = detectMimeType(filePath);

        // Step 1: Start resumable upload
        HttpHeaders startHeaders = new HttpHeaders();
        startHeaders.set("X-Goog-Upload-Protocol", "resumable");
        startHeaders.set("X-Goog-Upload-Command", "start");
        startHeaders.set("X-Goog-Upload-Header-Content-Length", String.valueOf(fileBytes.length));
        startHeaders.set("X-Goog-Upload-Header-Content-Type", mimeType);
        startHeaders.setContentType(MediaType.APPLICATION_JSON);

        String startUrl = GEMINI_API_BASE + "/files?key=" + apiKey;
        JsonObject startBody = new JsonObject();
        JsonObject fileObj = new JsonObject();
        fileObj.addProperty("display_name", file.getName());
        startBody.add("file", fileObj);

        HttpEntity<String> startRequest = new HttpEntity<>(gson.toJson(startBody), startHeaders);
        ResponseEntity<String> startResponse = restTemplate.exchange(startUrl, HttpMethod.POST, startRequest, String.class);
        String uploadUrl = startResponse.getHeaders().getFirst("X-Goog-Upload-URL");

        // Step 2: Upload the file bytes
        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.set("X-Goog-Upload-Offset", "0");
        uploadHeaders.set("X-Goog-Upload-Command", "upload, finalize");
        uploadHeaders.setContentType(MediaType.parseMediaType(mimeType));

        HttpEntity<byte[]> uploadRequest = new HttpEntity<>(fileBytes, uploadHeaders);
        ResponseEntity<String> uploadResponse = restTemplate.exchange(uploadUrl, HttpMethod.POST, uploadRequest, String.class);

        JsonObject responseJson = gson.fromJson(uploadResponse.getBody(), JsonObject.class);
        return responseJson.getAsJsonObject("file").get("uri").getAsString();
    }

    private String callInteractionsAPI(String fileUri, String languageHint, String mimeType) {
        String url = GEMINI_API_BASE + "/interactions?key=" + apiKey;

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "models/" + modelName);

        JsonArray inputArray = new JsonArray();
        JsonObject audioInput = new JsonObject();
        audioInput.addProperty("type", "audio");
        audioInput.addProperty("uri", fileUri);
        audioInput.addProperty("mime_type", mimeType);
        inputArray.add(audioInput);

        // 加入文字指令
        JsonObject textInput = new JsonObject();
        textInput.addProperty("type", "text");
        textInput.addProperty("text", languageHint +
            "Transcribe this audio with timestamps in SRT format. " +
            "Include speaker labels if multiple speakers are detected.");
        inputArray.add(textInput);

        requestBody.add("input", inputArray);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> httpEntity = new HttpEntity<>(gson.toJson(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);

        // 解析回應取得轉錄文字
        JsonObject responseJson = gson.fromJson(response.getBody(), JsonObject.class);
        // Interactions API 回應格式解析
        if (responseJson.has("output")) {
            JsonArray outputs = responseJson.getAsJsonArray("output");
            StringBuilder sb = new StringBuilder();
            for (JsonElement output : outputs) {
                JsonObject outputObj = output.getAsJsonObject();
                if (outputObj.has("text")) {
                    sb.append(outputObj.get("text").getAsString());
                }
            }
            return sb.toString();
        }
        return responseJson.toString();
    }

    private String detectMimeType(String filePath) {
        String name = filePath.toLowerCase();
        if (name.endsWith(".mp3")) return "audio/mp3";
        if (name.endsWith(".wav")) return "audio/wav";
        if (name.endsWith(".m4a")) return "audio/mp4";
        if (name.endsWith(".flac")) return "audio/flac";
        if (name.endsWith(".ogg")) return "audio/ogg";
        if (name.endsWith(".webm")) return "audio/webm";
        return "audio/mpeg";
    }
}
