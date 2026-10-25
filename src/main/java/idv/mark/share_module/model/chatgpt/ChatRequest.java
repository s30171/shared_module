package idv.mark.share_module.model.chatgpt;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {
    private String model;
    private List<Message> messages;
    private int n;
    private double temperature = 0;
    @JsonProperty("response_format")
    private Map<String, Object> responseFormat;
    @JsonProperty("max_tokens")
    private Integer maxTokens = 1300;

    public ChatRequest(String model, String prompt, int n) {
        this.model = model;
        this.n = n;
        this.messages = new ArrayList<>();
        this.messages.add(new Message("user", prompt));
    }

    public ChatRequest(String model, String systemPrompt, String userPrompt, int n, double temperature) {
        this.model = model;
        this.n = n;
        this.messages = new ArrayList<>();
        if (StringUtils.isNotBlank(systemPrompt)) {
            this.messages.add(new Message("system", systemPrompt));
        }
        this.messages.add(new Message("user", userPrompt));
        if (temperature > 0) {
            this.temperature = temperature;
        }
    }

    public ChatRequest(String model, String systemPrompt, String userPrompt, int n, double temperature, Map<String, Object> responseFormat) {
        this.model = model;
        this.n = n;
        this.messages = new ArrayList<>();
        if (StringUtils.isNotBlank(systemPrompt)) {
            this.messages.add(new Message("system", systemPrompt));
        }
        this.messages.add(new Message("user", userPrompt));
        if (temperature > 0) {
            this.temperature = temperature;
        }
        if (responseFormat != null) {
            this.responseFormat = responseFormat;
        }
    }


    // 推理模型（o 系列、gpt-5 系列）不接受 temperature 與 max_tokens
    private boolean isReasoningModel() {
        return model.contains("o1") || model.contains("o3") || model.startsWith("o4") || model.startsWith("gpt-5");
    }

    // 使用 @JsonGetter 並搭配 @JsonInclude 當回傳 null 時不序列化該 key
    @JsonGetter("temperature")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getTemperature() {
        if (isReasoningModel()) {
            return null;
        }
        return temperature;
    }

    @JsonGetter("max_tokens")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Integer getMaxTokens() {
        if (isReasoningModel()) {
            return null;
        }

        return maxTokens;
    }

    // 推理模型的輸出上限包含推理 token，保留四倍空間避免譯文被截斷
    @JsonGetter("max_completion_tokens")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Integer getMaxCompletionTokens() {
        if (!isReasoningModel() || maxTokens == null) {
            return null;
        }
        return maxTokens * 4;
    }

    // 翻譯不需要深度推理，降低推理強度以節省費用與時間（o1 不支援此參數）
    @JsonGetter("reasoning_effort")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getReasoningEffort() {
        if (!isReasoningModel() || model.contains("o1")) {
            return null;
        }
        return "low";
    }
}
