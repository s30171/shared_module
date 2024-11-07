package idv.mark.share_module.util;

import idv.mark.share_module.config.ConfigHelper;
import idv.mark.share_module.config.RemoteApiUrlConfig;
import idv.mark.share_module.model.chatgpt.LLMPromptRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class GrokUtil {

    public ResponseEntity<String> promptWithReq(String model, String promptText) {
        if (promptText.length() > 100) {
            String showPromptText = promptText.substring(0, 100) + "...";
            showPromptText += promptText.substring(promptText.length() - 100);
            log.info("promptText: {}", showPromptText);
        } else {
            log.info("promptText: {}", promptText);
        }
        if (StringUtils.isAllBlank(model)) {
            model = "grok-beta";
        }

        LLMPromptRequest request = new LLMPromptRequest(model, promptText, ConfigHelper.getBean(RemoteApiUrlConfig.class).getPass());
        String url = String.format("%s/api/grok/prompt", ConfigHelper.getBean(RemoteApiUrlConfig.class).getCrawUrl());
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

    public ResponseEntity<String> promptWithReq(String promptText) {
        return promptWithReq(null, promptText);
    }
}
