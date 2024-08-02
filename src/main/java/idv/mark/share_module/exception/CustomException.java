package idv.mark.share_module.exception;

import idv.mark.share_module._enum.ResponseEnum;

import java.util.function.Supplier;

public class CustomException extends RuntimeException implements Supplier<CustomException> {

    private String errorMsg;
    private String errorCode;
    private ResponseEnum responseEnum;

    public CustomException(ResponseEnum responseEnum) {
        this.errorMsg = responseEnum.getStatusMessage();
        this.errorCode = responseEnum.getStatusCode();
        this.responseEnum = responseEnum;
    }

    public CustomException(String enumCodeMsg) {
        super(enumCodeMsg);
        this.errorMsg = enumCodeMsg;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public ResponseEnum getResponseEnum() {
        return responseEnum;
    }

    @Override
    public CustomException get() {
        return this;
    }
}
