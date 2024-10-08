package idv.mark.share_module.util;

import idv.mark.share_module.model.chatgpt.ChatGPTPromptRequest;
import idv.mark.share_module.model.chatgpt.ChatRequest;
import idv.mark.share_module.model.chatgpt.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class ChatGPTUtil {

    @Value("${chatgpt.api-url}")
    private String gptApiUrl;
    @Value("${chatgpt.api-key}")
    private String gptApiKey;
    @Value("${craw.api-url}")
    private String crawApiUrl;
    @Value("${craw.pass}")
    private String pass;
    private static final String defaultModel = "gpt-4o-mini";

    public String prompt(String model, String promptText) {
        log.info("promptText: {}", promptText);
        ChatRequest request = new ChatRequest(model, promptText, 1);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/json");
        httpHeaders.add("Authorization", "Bearer " + gptApiKey);
        HttpEntity<ChatRequest> chatRequestHttpEntity = new HttpEntity<>(request, httpHeaders);
        ResponseEntity<ChatResponse> responseEntity = new RestTemplate().exchange(gptApiUrl, HttpMethod.POST, chatRequestHttpEntity, ChatResponse.class);
        ChatResponse body = responseEntity.getBody();
        log.info("ChatResponse: {}", body);
        return body.getChoices().get(0).getMessage().getContent();
    }

    public ResponseEntity<String> promptWithReq(String model, String promptText) {
        log.info("promptText: {}", promptText);
        ChatGPTPromptRequest request = new ChatGPTPromptRequest(model, promptText, pass);
        ResponseEntity<String> response = RESTUtil.restTemplate.postForEntity(crawApiUrl, request, String.class);
        String body = response.getBody();
        log.info("ChatResponse: {}", body);
        return response;
    }

    public ResponseEntity<String> promptWithReq(String promptText) {
        return promptWithReq(defaultModel, promptText);
    }
}
