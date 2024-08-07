package idv.mark.share_module.model.translate;

import idv.mark.share_module._enum.LanguageEnum;
import lombok.Data;

@Data
public class TranslateModel {
    private String sourceText;
    private LanguageEnum sourceLanguage;
    private TranslateSourceEnum translateSource = TranslateSourceEnum.DeepL;
    private SpecialConvertEnum specialConvert;
    private LanguageEnum targetLanguage;
    private String targetText;
}
