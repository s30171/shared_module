package idv.mark.share_module._aop;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class SLf4RequestResponseLoggingAspect {

    private static final Gson gson = new Gson();

    @Before("@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void sl4logRequest(JoinPoint joinPoint) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            int lastPointIndex = joinPoint.getSignature().getDeclaringTypeName().lastIndexOf(".") + 1;
            String class_method = joinPoint.getSignature().getDeclaringTypeName().substring(lastPointIndex) + "." +
                    joinPoint.getSignature().getName() + "()";
            // 印出 Request Parameters
            Map<String, String[]> parameterMap = request.getParameterMap();
            String parameters = parameterMap.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + Arrays.toString(entry.getValue()))
                    .collect(Collectors.joining(", "));
            String method = request.getMethod();
            if ("GET".equals(method)) {
                log.info("method: [{}], requestUrl: [{}], classMethod: [{}], Request Parameters: [{}]", request.getMethod(), request.getRequestURL(), class_method, parameters);
            } else {
                Object body = joinPoint.getArgs().length >= 1 ? joinPoint.getArgs()[0] : "";
                log.info("method: [{}], requestUrl: [{}], classMethod: [{}], Request Parameters: [{}], Body: [{}]", request.getMethod(), request.getRequestURL(), class_method, parameters, gson.toJson(body));
            }
        } catch (Exception e) {
            log.error("print request error", e);
        }
    } 


    @AfterReturning(pointcut =
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)",
            returning = "response")
    public void sl4logResponse(JoinPoint joinPoint, Object responseParam) {
        try {
            String append = "";
            if (responseParam instanceof ResponseEntity) {
                ResponseEntity<?> response = (ResponseEntity<?>) responseParam;
                Object body = response.getBody();
                append += body;
                log.info("Response type: {}, body: {}", responseParam.getClass(), append);
            }
        } catch (Exception e) {
            log.error("print response error", e);
        }
    }
}
