package idv.mark.share_module.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Slf4j
@Configuration
@PropertySource("classpath:zipkin.yml")
public class ZipkinConfig {
    // 讀取resource/zipkin.yml設定檔

    public ZipkinConfig() {
        log.info("ZipkinConfig init");
    }
}
