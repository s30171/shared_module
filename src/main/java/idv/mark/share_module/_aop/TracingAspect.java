package idv.mark.share_module._aop;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TracingAspect {

    private final Tracer tracer;

    // 定義一個 Pointcut，攔截所有使用了 TraceAsync 的方法
    @Pointcut("@annotation(TraceAsync)")
    public void annotatedWithTracingAsync() {
    }

    @Around("annotatedWithTracingAsync()")
    public Object wrapAsyncMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        CurrentTraceContext currentTraceContext = tracer.currentTraceContext();
        return currentTraceContext.wrap(() -> {
            Span customCreatedSpan = tracer.nextSpan().name("customCreatedSpan");
            try (Tracer.SpanInScope ws = tracer.withSpan(customCreatedSpan.start())) {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }).call();
    }
}
