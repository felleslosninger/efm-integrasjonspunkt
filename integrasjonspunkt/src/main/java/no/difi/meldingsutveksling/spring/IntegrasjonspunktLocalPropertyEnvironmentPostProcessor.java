package no.difi.meldingsutveksling.spring;


import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author kons-nlu
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class IntegrasjonspunktLocalPropertyEnvironmentPostProcessor implements EnvironmentPostProcessor, ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IntegrasjonspunktLocalPropertyEnvironmentPostProcessor.class);
    
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            final FileSystemResource resource = new FileSystemResource("integrasjonspunkt-local.properties");
            Properties loadAllProperties = PropertiesLoaderUtils.loadProperties(resource);
            environment.getPropertySources().addFirst(new PropertiesPropertySource("file:integrasjonspunkt-local.properties", loadAllProperties));
            log.info("Added " + resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.error("Failed to load integrasjonspunkt-local.properties", ex);
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent e) {
        this.postProcessEnvironment(e.getEnvironment(), e.getSpringApplication());
    }
    
}
