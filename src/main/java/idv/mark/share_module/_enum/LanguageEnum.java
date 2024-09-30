package idv.mark.share_module._enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LanguageEnum {
    CHINESE("ZH-HANT"),
//    SIMPLIFIED_CHINESE("ZH-HANS"),
    ENGLISH("EN"),
    JAPANESE("JA"),
    KOREAN("KO"),
    THAI("TH"),
    ;
    private String code;

    public static LanguageEnum getByISOCode(String code) {
        for (LanguageEnum languageEnum : LanguageEnum.values()) {
            if (languageEnum.getCode().equalsIgnoreCase(code)) {
                return languageEnum;
            }
        }
        return null;
    }
}
