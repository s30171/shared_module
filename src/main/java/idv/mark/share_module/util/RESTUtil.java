package idv.mark.share_module.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RESTUtil implements ApplicationContextAware {

    private static ApplicationContext context;
    public static RestTemplate restTemplate = getRestTemplate();

    private static RestTemplate getRestTemplate() {
        return context.getBean(RestTemplate.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
}
