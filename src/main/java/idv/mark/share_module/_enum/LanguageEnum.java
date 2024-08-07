package idv.mark.share_module._enum;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LanguageEnum {
    CHINESE("ZH-HANT"),
    SIMPLIFIED_CHINESE("ZH-HANS"),
    ENGLISH("EN-US"),
    JAPANESE("JA"),
    KOREAN("KO"),
    THAI("TH"),
    ;
    private String code;
}
