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
    private Integer maxTokens = 2000;

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


    // 使用 @JsonGetter 並搭配 @JsonInclude 當回傳 null 時不序列化該 key
    @JsonGetter("temperature")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Double getTemperature() {
        // 如果模型是 o1 或 o3，則不傳送 temperature
        if (model.contains("o1") || model.contains("o3")) {
            return null;
        }
        return temperature;
    }

    @JsonGetter("max_tokens")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Integer getMaxTokens() {
        // 如果模型是 o1 或 o3，則不傳送 temperature
        if (model.contains("o1") || model.contains("o3")) {
            return null;
        }

        return maxTokens;
    }
}
