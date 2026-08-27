package idv.mark.share_module.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "idv.mark.share_module.task")
@EnableJpaRepositories(basePackages = "idv.mark.share_module.task")
public class JpaConfig {
}
