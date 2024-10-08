package idv.mark.share_module.model.chatgpt;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
public class ChatGPTPromptRequest {
    private String model = "gpt-4o-mini";
    private String postBody;
    private String pass;

    public ChatGPTPromptRequest(String model, String postBody, String pass) {
        this.postBody = postBody;
        if (StringUtils.isNotBlank(model)) {
            this.model = model;
        }
        this.pass = pass;
    }
}
