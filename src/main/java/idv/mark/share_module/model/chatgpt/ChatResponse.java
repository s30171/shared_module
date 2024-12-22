package idv.mark.share_module.model.chatgpt;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class ChatResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("object")
    private String object;
    @SerializedName("created")
    private long created;
    @SerializedName("model")
    private String model;
    @SerializedName("choices")
    private List<Choice> choices;
    @SerializedName("usage")
    private Usage usage;
    @SerializedName("system_fingerprint")
    private String systemFingerprint;

    @Data
    public static class Choice {
        @SerializedName("index")
        private int index;
        @SerializedName("message")
        private Message message;
        @SerializedName("logprobs")
        private Object logprobs;
        @SerializedName("finish_reason")
        private String finishReason;
    }

    @Data
    public static class Message {
        @SerializedName("role")
        private String role;
        @SerializedName("content")
        private String content;
        @SerializedName("refusal")
        private Object refusal;
    }

    @Data
    public static class Usage {
        @SerializedName("prompt_tokens")
        private int promptTokens;
        @SerializedName("completion_tokens")
        private int completionTokens;
        @SerializedName("total_tokens")
        private int totalTokens;
        @SerializedName("prompt_tokens_details")
        private PromptTokensDetails promptTokensDetails;
        @SerializedName("completion_tokens_details")
        private CompletionTokensDetails completionTokensDetails;
    }

    @Data
    public static class PromptTokensDetails {
        @SerializedName("cached_tokens")
        private int cachedTokens;
        @SerializedName("audio_tokens")
        private int audioTokens;
    }

    @Data
    public static class CompletionTokensDetails {
        @SerializedName("reasoning_tokens")
        private int reasoningTokens;
        @SerializedName("audio_tokens")
        private int audioTokens;
        @SerializedName("accepted_prediction_tokens")
        private int acceptedPredictionTokens;
        @SerializedName("rejected_prediction_tokens")
        private int rejectedPredictionTokens;
    }
}

