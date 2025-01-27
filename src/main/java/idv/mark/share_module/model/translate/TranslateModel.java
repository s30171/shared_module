package idv.mark.share_module.model.translate;

import idv.mark.share_module._enum.LanguageEnum;
import idv.mark.share_module._enum.TranslateSourceSetting;
import idv.mark.share_module.model.craw.SRTModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TranslateModel {
    private String sourceText;
    private LanguageEnum sourceLanguage;
    private TranslateSourceEnum translateSource;
    private String gptModelName;
    private double gptTemperature = 0.1;
    private SpecialConvertEnum specialConvert;
    private LanguageEnum targetLanguage;
    private String targetText;

    public TranslateModel(Double gptTemperature,
                          String modelName,
                          TranslateSourceEnum translateSourceEnum,
                          SpecialConvertEnum specialConvertEnum,
                          LanguageEnum sourceLanguageEnum,
                          LanguageEnum targetLanguageEnum,
                          String translateString) {
        if (specialConvertEnum != null) {
            this.specialConvert = specialConvertEnum;
        }
        if (StringUtils.isNotBlank(modelName)) {
            this.gptModelName = modelName;
        }
        if (gptTemperature != null) {
            this.gptTemperature = gptTemperature;
        }
        this.sourceLanguage = sourceLanguageEnum;
        this.sourceText = translateString;
        this.targetLanguage = targetLanguageEnum;
        this.translateSource = Objects.isNull(translateSourceEnum) ? TranslateSourceEnum.ChatGPT : translateSourceEnum;
    }

    public void setBySRTModel(List<SRTModel> srtModels) {
        if (CollectionUtils.isEmpty(srtModels)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (SRTModel srtModel : srtModels) {
            sb.append(srtModel.getText()).append("\n");
        }
        this.sourceText = sb.toString();
    }

    public void setByTranslateSourceSetting() {
        // 如果沒有設定翻譯來源, 則取得預設翻譯來源
        if (this.translateSource == null) {
            this.translateSource = TranslateSourceSetting.getTranslateSource(this.sourceLanguage, this.targetLanguage);
        }
    }
}
