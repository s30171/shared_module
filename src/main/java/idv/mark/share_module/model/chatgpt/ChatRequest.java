package idv.mark.share_module.model.chatgpt;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatRequest {
    private String model;
    private List<Message> messages;
    private int n;
    private double temperature = 0.3;

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
}
