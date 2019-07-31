package no.difi.meldingsutveksling.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationEvent;
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
public class IntegrasjonspunktLocalPropertyEnvironmentPostProcessor implements EnvironmentPostProcessor, ApplicationListener<ApplicationEvent> {

    private static final DeferredLog log = new DeferredLog();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.getProperty("app.local.properties.enable", Boolean.class, true)) {
            log.info("Disable local properties file for test");
            return;
        }
        try {
            final FileSystemResource resource = new FileSystemResource("integrasjonspunkt-local.properties");
            Properties loadAllProperties = PropertiesLoaderUtils.loadProperties(resource);
            environment.getPropertySources().addFirst(new PropertiesPropertySource("file:integrasjonspunkt-local.properties", loadAllProperties));
            log.info("Added " + resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            if (!"mock".equals(environment.getProperty("spring.profiles.active"))) {
                log.error("Failed to load integrasjonspunkt-local.properties - exiting.", ex);
            }
        }
    }

    @Override
    public void onApplicationEvent(ApplicationEvent e) {
        log.replayTo(IntegrasjonspunktLocalPropertyEnvironmentPostProcessor.class);
        if (e instanceof ApplicationEnvironmentPreparedEvent) {
            ApplicationEnvironmentPreparedEvent ee = (ApplicationEnvironmentPreparedEvent) e;
            this.postProcessEnvironment(ee.getEnvironment(), ee.getSpringApplication());
        }
    }

}
