package idv.mark.share_module.model.chatgpt;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
public class LLMPromptRequest {
    private String model = "gpt-4o-mini";
    private String systemPrompt;
    private String postBody;
    private String pass;
    private double temperature = 0.3;

    public LLMPromptRequest(String model, String postBody, String pass) {
        this.postBody = postBody;
        if (StringUtils.isNotBlank(model)) {
            this.model = model;
        }
        this.pass = pass;
    }

    public LLMPromptRequest(String model, String systemPrompt, String postBody, double temperature, String pass) {
        this.postBody = postBody;
        this.systemPrompt = systemPrompt;
        if (StringUtils.isNotBlank(model)) {
            this.model = model;
        }
        this.pass = pass;
    }
}
