package idv.mark.share_module._enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LanguageEnum {
    CHINESE("ZH-HANT"),
    ENGLISH("EN-US"),
    JAPANESE("JA"),
    KOREAN("KO"),
    THAI("TH"),
    VIETNAMESE("VI"),
    INDONESIAN("ID"),
    SPANISH("ES"),
    ;
    private String code;

    public static LanguageEnum getByISOCode(String code) {
        for (LanguageEnum languageEnum : LanguageEnum.values()) {
            if (languageEnum.getCode().equalsIgnoreCase(code)) {
                return languageEnum;
            }
        }
        if ("en".equals(code)) {
            return ENGLISH;
        }
        if ("zh".equals(code)) {
            return CHINESE;
        }
        return null;
    }

    public String getSpiltISOCode() {
        if (code.contains("-")) {
            return code.split("-")[0].toLowerCase();
        }
        return code.toLowerCase();
    }
}
