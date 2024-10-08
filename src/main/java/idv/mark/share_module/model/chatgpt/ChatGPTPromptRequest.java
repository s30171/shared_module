package idv.mark.share_module.model.chatgpt;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
public class ChatGPTPromptRequest {
    private String postBody;
    private String model = "gpt-4o-mini";
    private String pass;

    public ChatGPTPromptRequest(String postBody, String model, String pass) {
        this.postBody = postBody;
        if (StringUtils.isNotBlank(model)) {
            this.model = model;
        }
        this.pass = pass;
    }
}
