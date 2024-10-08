package idv.mark.share_module.model.chatgpt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatGPTPromptRequest {
    private String postBody;
    private String model;
    private String pass;
}
