package idv.mark.share_module.model.chatgpt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    private String systemFingerprint;

    @Data
    public static class Choice {
        private int index;
        private Message message;
        private String finishReason;
    }

    @Data
    public static class Message {
        private String role;
        private String content;
        private String refusal;
    }

    @Data
    public static class Usage {
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
        private TokenDetails promptTokensDetails;
        private TokenDetails completionTokensDetails;
    }

    @Data
    public static class TokenDetails {
        private int cachedTokens;
        private int audioTokens;
        private int reasoningTokens;
        private int acceptedPredictionTokens;
        private int rejectedPredictionTokens;
    }
}

