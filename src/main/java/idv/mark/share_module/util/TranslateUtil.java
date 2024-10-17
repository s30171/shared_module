package idv.mark.share_module.util;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import idv.mark.share_module._enum.LanguageEnum;
import idv.mark.share_module.config.ConfigHelper;
import idv.mark.share_module.model.craw.SRTModel;
import idv.mark.share_module.model.translate.SpecialConvertEnum;
import idv.mark.share_module.model.translate.TranslateModel;
import idv.mark.share_module.model.translate.TranslateSourceEnum;
import idv.mark.share_module.res.JsonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranslateUtil {

    @Value("{translate.url:{null}}")
    private String translateUrl;

    public TranslateModel translate(TranslateModel request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = new Gson().toJson(request);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        Type type = new TypeToken<JsonResponse<TranslateModel>>() {}.getType();
        ResponseEntity<String> translateModelResponseEntity = ConfigHelper.getBean(RestTemplate.class).postForEntity(translateUrl, entity, String.class);
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

    // 將SRT文字轉換為SRTModel
    public static List<SRTModel> convertToSRTModel(String text) {
        if (StringUtils.isAllBlank(text)) {
            return null;
        }
        List<SRTModel> srtModels = new ArrayList<>();
        String[] split = text.split("\n");

        // 刪除空行
        int startLine = 0;
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].trim();
            // 判斷第一行在哪
            if (StringUtils.isNotBlank(split[i])) {
                try {
                    Integer.parseInt(split[i]);
                } catch (NumberFormatException e) {
                    continue;
                }
                startLine = i;
                break;
            }
        }
        split = Arrays.copyOfRange(split, startLine, split.length);

        // 將SRT文字分成區塊
        List<String> currentBlock = new ArrayList<>();
        for (int i = 0; i < split.length; i++) {
            String line = split[i];
            // 如果是新的編號行且currentBlock不為空，則處理並清空currentBlock
            if (line.matches("\\d+") && !currentBlock.isEmpty()) {
                srtModels.add(convertBlockToSRTModel(currentBlock));
                // 在這裡處理完整的SRT區塊
                currentBlock.clear();
            }

            // 將當前行加入currentBlock
            currentBlock.add(line);

            // 處理最後一行結束時的情況
            if (i == split.length - 1) {
                // 處理最後一個SRT區塊
                try {
                    srtModels.add(convertBlockToSRTModel(currentBlock));
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        }

        return srtModels;
    }

    // 將SRTModel轉換為SRT文字
    public static String srtModelToText(List<SRTModel> srtModels) {
        if (CollectionUtils.isEmpty(srtModels)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (SRTModel srtModel : srtModels) {
            sb.append(srtModel.getSequence()).append("\n");
            sb.append(srtModel.getTime()).append("\n");
            sb.append(srtModel.getText()).append("\n");
            sb.append(srtModel.getLineBreak()).append("\n");
        }
        return sb.toString();
    }

    // 將SRTModel轉換為SRT文字
    public static String srtModelsExtractText(List<SRTModel> srtModels) {
        return CollectionUtils.isEmpty(srtModels) ? "" : srtModels.stream().map(SRTModel::getText).collect(Collectors.joining("\n"));
    }

    private static SRTModel convertBlockToSRTModel(List<String> block) {
        String text = block.size() > 4 ? String.join("\n", block.subList(2, block.size())) : block.get(2);
        // 移除最後一個換行符號（如果存在）
        if (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        return new SRTModel(Integer.parseInt(block.get(0)), block.get(1), text, "\n");
    }
}
