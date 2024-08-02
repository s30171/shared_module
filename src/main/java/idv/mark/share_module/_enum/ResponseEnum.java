package idv.mark.share_module._enum;

import lombok.Getter;

@Getter
public enum ResponseEnum {

    SUCCESS("S000", "SUCCESS", 200),
    FAIL("F000", "ERROR", 500),
    FILE_NOT_FOUND("F001", "File not found", 500),
    ENUM_SETTING_NOT_FOUND("F002", "Enum setting not found", 500),
    ;

    private String statusCode;
    private String statusMessage;
    private Integer httpStatusCode;

    ResponseEnum(String statusCode, String statusMessage, Integer httpStatusCode) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.httpStatusCode = httpStatusCode;
    }
}
