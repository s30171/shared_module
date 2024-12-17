package idv.mark.share_module.util;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import idv.mark.share_module._enum.LanguageEnum;
import idv.mark.share_module.config.ConfigHelper;
import idv.mark.share_module.config.RemoteApiUrlConfig;
import idv.mark.share_module.model.craw.SRTModel;
import idv.mark.share_module.model.translate.SpecialConvertEnum;
import idv.mark.share_module.model.translate.TranslateModel;
import idv.mark.share_module.model.translate.TranslateSourceEnum;
import idv.mark.share_module.res.JsonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranslateUtil {

    public TranslateModel translate(TranslateModel request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = new Gson().toJson(request);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        Type type = new TypeToken<JsonResponse<TranslateModel>>() {}.getType();
        String url = String.format("%s/api/translate", ConfigHelper.getBean(RemoteApiUrlConfig.class).getTranslateUrl());
        ResponseEntity<String> translateModelResponseEntity = ConfigHelper.getBean(RestTemplate.class).postForEntity(url, entity, String.class);
        String body = translateModelResponseEntity.getBody();
        JsonResponse<TranslateModel> translateModelJsonResponse = new Gson().fromJson(body, type);
        return translateModelJsonResponse.getData();
    }

    public TranslateModel translate(TranslateSourceEnum translateSource, SpecialConvertEnum specialConvertEnum, LanguageEnum sourceLanguageEnum, LanguageEnum targetLanguageEnum, String translateString) {
        TranslateModel request = new TranslateModel();
        if (translateSource != null) {
            request.setTranslateSource(translateSource);
        }
        if (specialConvertEnum != null) {
            request.setSpecialConvert(specialConvertEnum);
        }
        request.setSourceLanguage(sourceLanguageEnum);
        request.setSourceText(translateString);
        request.setTargetLanguage(targetLanguageEnum);
        return translate(request);
    }

    public TranslateModel translateByGPT(Double gptTemperature, String modelName, SpecialConvertEnum specialConvertEnum, LanguageEnum sourceLanguageEnum, LanguageEnum targetLanguageEnum, String translateString) {
        TranslateModel request = getTranslateByGPTRequest(gptTemperature, modelName, null, specialConvertEnum, sourceLanguageEnum, targetLanguageEnum, translateString);
        return translate(request);
    }

    public TranslateModel translateByGPTSRT(Double gptTemperature, String modelName, SpecialConvertEnum specialConvertEnum, LanguageEnum sourceLanguageEnum, LanguageEnum targetLanguageEnum, String translateString) {
        TranslateModel translateByGPTRequest = getTranslateByGPTRequest(gptTemperature, modelName, TranslateSourceEnum.ChatGPT_SRT, specialConvertEnum, sourceLanguageEnum, targetLanguageEnum, translateString);
        return translate(translateByGPTRequest);
    }

    // 偵測字幕語言
    public LanguageEnum detectLanguage(List<SRTModel> srtModels) {
        List<List<SRTModel>> partition = Lists.partition(srtModels, 30);
        List<SRTModel> srtModelList = partition.get(0);
        String appendRequestString = srtModelList.stream()
                .collect(StringBuilder::new, (builder, element) -> builder.append(element.getText()).append("\n"), StringBuilder::append)
                .toString();
        appendRequestString += "\n ```\n";
        String prompt = appendRequestString + "這一段文字是什麼語言? 直接給我{ISO-639 code}格式, 依最多出現語言, 回覆一種就好, 例如日語 {ja}, 韓文 {ko}, 英文 {en-us}, 中文 {zh-hant}..., 一定要符合ISO-639格式";
        ResponseEntity<String> responseEntity = ConfigHelper.getBean(ChatGPTUtil.class).promptWithReq(prompt);
        String replace = responseEntity.getBody().replace("{", "").replace("}", "");
        return LanguageEnum.getByISOCode(replace);
    }

    private TranslateModel getTranslateByGPTRequest(Double gptTemperature, String modelName, TranslateSourceEnum translateSourceEnum, SpecialConvertEnum specialConvertEnum, LanguageEnum sourceLanguageEnum, LanguageEnum targetLanguageEnum, String translateString) {
        TranslateModel request = new TranslateModel();
        if (specialConvertEnum != null) {
            request.setSpecialConvert(specialConvertEnum);
        }
        if (StringUtils.isNotBlank(modelName)) {
            request.setGptModelName(modelName);
        }
        if (gptTemperature != null) {
            request.setGptTemperature(gptTemperature);
        }
        request.setSourceLanguage(sourceLanguageEnum);
        request.setSourceText(translateString);
        request.setTargetLanguage(targetLanguageEnum);
        request.setTranslateSource(Objects.isNull(translateSourceEnum) ? TranslateSourceEnum.ChatGPT : translateSourceEnum);
        return request;
    }
}
