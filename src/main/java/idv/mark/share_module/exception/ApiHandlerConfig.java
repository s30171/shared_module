package idv.mark.share_module.exception;

import idv.mark.share_module._enum.ResponseEnum;
import idv.mark.share_module.res.JsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ApiHandlerConfig extends ResponseEntityExceptionHandler {

    @ResponseBody
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<JsonResponse> exceptionHandler(CustomException e) {
        if (200 != e.getResponseEnum().getHttpStatusCode()) {
            log.error("System Error: ", e);
            return ResponseEntity.status(e.getResponseEnum().getHttpStatusCode()).body(JsonResponse.of(e.getResponseEnum()));
        }
        return ResponseEntity.ok().body(JsonResponse.of(e.getResponseEnum()));
    }

    @ResponseBody
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String errorMessage = e.getBindingResult().getAllErrors()
                .stream()
                .map(objectError -> objectError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.error("handleMethodArgumentNotValid Error: {}", errorMessage);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(JsonResponse.of(ResponseEnum.ENUM_SETTING_NOT_FOUND));
    }

    @ResponseBody
    @ExceptionHandler(Exception.class)
    protected ResponseEntity handleExceptionInternal(HttpServletRequest request,
                                                     HttpServletResponse response,
                                                     Exception e) {
        log.error("####################### UnExcepted Error Start #######################");
        log.error("System Error: ", e);
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        if (request.getCookies() != null) {
            Optional.ofNullable(Arrays.stream(request.getCookies()).collect(Collectors.toList()))
                    .orElse(Collections.emptyList())
                    .stream()
                    .forEach(cookie -> sb.append("Cookie Name - " + cookie.getName() + ", Value - " + cookie.getValue() + "\n"));
        }
        Enumeration<String> headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            sb.append("Header Name - " + headerName + ", Value - " + request.getHeader(headerName) + "\n");
        }
        Enumeration<String> params = request.getParameterNames();
        while(params.hasMoreElements()){
            String paramName = params.nextElement();
            sb.append("Parameter Name - " + paramName + ", Value - " + request.getParameter(paramName) + "\n");
        }
        sb.append("Query String - " + request.getQueryString() + "\n");
        sb.append("Request URL - " + request.getRequestURL().toString() + "\n");
        if (HttpMethod.GET.toString().equals(request.getMethod())) {
            sb.append("Http Method -> 'GET' Request URL - " + request.getRequestURL().toString() + "?" + request.getQueryString() + "\n");
        }
        log.error(sb.toString());
        log.error("####################### UnExcepted Error End #######################");
        return ResponseEntity.status(500).body(JsonResponse.of(ResponseEnum.FAIL));
    }
}