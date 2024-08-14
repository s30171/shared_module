package idv.mark.share_module.config;


import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

public class LogbackContextInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        String appName = environment.getProperty("spring.application.name");

        Properties properties = new Properties();
        properties.put("APP_NAME", appName);
        properties.put("appName", appName);

        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new PropertiesPropertySource("logbackConfig", properties));
    }
}

