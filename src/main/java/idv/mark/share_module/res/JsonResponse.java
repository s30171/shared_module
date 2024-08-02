package idv.mark.share_module.res;

import idv.mark.share_module._enum.ResponseEnum;
import lombok.Getter;

@Getter
public class JsonResponse<T> {

    private String statusCode;
    private String message;
    private boolean success;
    private T data;
    private Long serverTime = System.currentTimeMillis();

    private JsonResponse(ResponseEnum responseEnum, T data) {
        this.message = responseEnum.getStatusMessage();
        this.statusCode = responseEnum.getStatusCode();
        this.success = judgeSuccess();
        this.data = data;
    }

    private JsonResponse(String statusCode, String message, T data) {
        this.message = message;
        this.statusCode = statusCode;
        this.success = judgeSuccess();
        this.data = data;
    }

    private boolean judgeSuccess() {
        return ResponseEnum.SUCCESS.getStatusCode().equals(this.statusCode);
    }

    public static <T> JsonResponse<T> of(ResponseEnum responseEnum, T data) {
        return new JsonResponse<>(responseEnum, data);
    }

    public static <T> JsonResponse<T> of(String statusCode, String message, T data) {
        return new JsonResponse<>(statusCode, message, data);
    }

    public static <T> JsonResponse<T> of(ResponseEnum responseEnum) {
        return new JsonResponse<>(responseEnum, null);
    }

    public static <T> JsonResponse<T> success(T data) {
        return new JsonResponse<>(ResponseEnum.SUCCESS, data);
    }

    public static <T> JsonResponse<T> success(String message, T data) {
        return new JsonResponse<>(ResponseEnum.SUCCESS.getStatusCode(), message, data);
    }

    public static <T> JsonResponse<T> error(ResponseEnum responseEnum, T data) {
        return new JsonResponse<>(responseEnum, data);
    }

    public static <T> JsonResponse<T> error(String message, T data) {
        return new JsonResponse<>(ResponseEnum.FAIL.getStatusCode(), message, data);
    }
}