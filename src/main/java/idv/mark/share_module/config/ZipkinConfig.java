package idv.mark.share_module.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Configuration
@AutoConfiguration
public class ZipkinConfig {
    // 讀取resource/zipkin.yml設定檔
    @Bean
    protected PropertySourcesPlaceholderConfigurer createPropertyConfigurer(ConfigurableEnvironment environment) {
        PropertySourcesPlaceholderConfigurer result = new PropertySourcesPlaceholderConfigurer();
        try {
            Resource resource = new ClassPathResource("zipkin.properties");
            result.setLocation(resource);
            try (InputStream is = resource.getInputStream()) {
                Properties properties = new Properties();
                properties.load(is);
                environment.getPropertySources().addFirst(new PropertiesPropertySource("zipkinProperties", properties));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
