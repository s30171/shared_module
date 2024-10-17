package idv.mark.share_module.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class RemoteApiUrlConfig {
    @Value("${api-remote.craw.url:{null}}")
    private String crawUrl;
    @Value("${api-remote.craw.pass:{null}}")
    private String pass;
    @Value("${api-remote.translate.url:{null}}")
    private String translateUrl; 
}
