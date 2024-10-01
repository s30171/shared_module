package idv.mark.share_module.model.translate;

import idv.mark.share_module._enum.LanguageEnum;
import idv.mark.share_module._enum.TranslateSourceSetting;
import idv.mark.share_module.model.craw.SRTModel;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Data
public class TranslateModel {
    private String sourceText;
    private LanguageEnum sourceLanguage;
    private TranslateSourceEnum translateSource;
    private SpecialConvertEnum specialConvert;
    private LanguageEnum targetLanguage;
    private String targetText;

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
