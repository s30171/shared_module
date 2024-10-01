package idv.mark.share_module._enum;

import idv.mark.share_module.model.translate.TranslateSourceEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Objects;

@Getter
@AllArgsConstructor
public enum TranslateSourceSetting {

    CHINESE(TranslateSourceEnum.DeepL, Map.of(
            LanguageEnum.THAI, TranslateSourceEnum.Azure,
            LanguageEnum.KOREAN, TranslateSourceEnum.Papago
    )),
    SIMPLIFIED_CHINESE(TranslateSourceEnum.DeepL, Map.of(
            LanguageEnum.THAI, TranslateSourceEnum.Google,
            LanguageEnum.KOREAN, TranslateSourceEnum.Papago
    )),
    ENGLISH(TranslateSourceEnum.Azure, Map.of(
            LanguageEnum.THAI, TranslateSourceEnum.Azure
    )),
    JAPANESE(TranslateSourceEnum.DeepL, Map.of(
            LanguageEnum.THAI, TranslateSourceEnum.Azure
    )),
    KOREAN(TranslateSourceEnum.DeepL, Map.of(
            LanguageEnum.CHINESE, TranslateSourceEnum.Papago,
            LanguageEnum.THAI, TranslateSourceEnum.Azure,
            LanguageEnum.ENGLISH, TranslateSourceEnum.Google
    )),
    THAI(TranslateSourceEnum.Azure, Map.of(
            LanguageEnum.CHINESE, TranslateSourceEnum.Papago
    )),
    ;

    private TranslateSourceEnum defaultTranslateSource;
    private Map<LanguageEnum, TranslateSourceEnum> translateSourceEnumMap;

    public static TranslateSourceEnum getTranslateSource(LanguageEnum sourceLanguage, LanguageEnum targetLanguage) {
        TranslateSourceEnum translateSourceEnum = null;
        for (TranslateSourceSetting setting : TranslateSourceSetting.values()) {
            LanguageEnum languageEnum = LanguageEnum.valueOf(setting.name());
            // 偵測同語言設定
            if (!Objects.equals(sourceLanguage, languageEnum)) {
                continue;
            }
            translateSourceEnum = setting.getDefaultTranslateSource();
            // 偵測目標語言設定 (如果有特規設定, 取Map值, 否則取預設值)
            if (setting.getTranslateSourceEnumMap().containsKey(targetLanguage)) {
                return setting.getTranslateSourceEnumMap().get(targetLanguage);
            }
        }
        return Objects.isNull(translateSourceEnum) ? TranslateSourceEnum.DeepL : translateSourceEnum;
    }
}
