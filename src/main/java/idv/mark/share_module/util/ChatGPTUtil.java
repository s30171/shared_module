package idv.mark.share_module.util;

import idv.mark.share_module.config.ConfigHelper;
import idv.mark.share_module.config.RemoteApiUrlConfig;
import idv.mark.share_module.model.chatgpt.LLMPromptRequest;
import idv.mark.share_module.model.chatgpt.ChatRequest;
import idv.mark.share_module.model.chatgpt.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ChatGPTUtil {

    @Value("${chatgpt.api-url:https://api.openai.com/v1/chat/completions}")
    private String gptApiUrl;
    @Value("${chatgpt.api-key:{null}}")
    private String gptApiKey;

    public String prompt(String model, String promptText) {
        if (promptText.length() > 100) {
            String showPromptText = promptText.substring(0, 100) + "...";
            showPromptText += promptText.substring(promptText.length() - 100);
            log.info("promptText: {}", showPromptText);
        } else {
            log.info("promptText: {}", promptText);
        }
        ChatRequest request = new ChatRequest(model, promptText, 1);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json");
        httpHeaders.add("Authorization", "Bearer " + gptApiKey);
        HttpEntity<ChatRequest> chatRequestHttpEntity = new HttpEntity<>(request, httpHeaders);
        ResponseEntity<ChatResponse> responseEntity = ConfigHelper.getBean(RestTemplate.class).exchange(gptApiUrl, HttpMethod.POST, chatRequestHttpEntity, ChatResponse.class);
        ChatResponse body = responseEntity.getBody();
        if (body != null) {
            String content = body.getChoices().get(0).getMessage().getContent();
            if (content.length() > 100) {
                String showBody = content.substring(0, 100) + "...";
                showBody += content.substring(content.length() - 100);
                log.info("ChatResponse: {}", showBody);
            } else {
                log.info("ChatResponse: {}", body);
            }
        }
        return body.getChoices().get(0).getMessage().getContent();
    }

    public ResponseEntity<String> promptWithReq(String model, String systemPrompt, String userPrompt) {
        if (userPrompt.length() > 100) {
            String showPromptText = userPrompt.substring(0, 100) + "...";
            showPromptText += userPrompt.substring(userPrompt.length() - 100);
            log.info("promptText: {}", showPromptText);
        } else {
            log.info("promptText: {}", userPrompt);
        }
        LLMPromptRequest request = new LLMPromptRequest(model, systemPrompt, userPrompt, ConfigHelper.getBean(RemoteApiUrlConfig.class).getPass());
        String url = String.format("%s/api/prompt", ConfigHelper.getBean(RemoteApiUrlConfig.class).getCrawUrl());
        ResponseEntity<String> response = ConfigHelper.getBean(RestTemplate.class).postForEntity(url, request, String.class);
        String body = response.getBody();
        if (body != null && body.length() > 100) {
            String showBody = body.substring(0, 100) + "...";
            showBody += body.substring(body.length() - 100);
            log.info("ChatResponse: {}", showBody);
        } else {
            log.info("ChatResponse: {}", body);
        }
        return response;
    }

    public ResponseEntity<String> promptWithReq(String model, String userPrompt) {
        return promptWithReq(model, null, userPrompt);
    }

    public ResponseEntity<String> promptWithReq(String promptText) {
        return promptWithReq(null, null, promptText);
    }
}
