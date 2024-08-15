package idv.mark.share_module.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:zipkin.yml")
public class ZipkinConfig {
    // 讀取resource/zipkin.yml設定檔
}
