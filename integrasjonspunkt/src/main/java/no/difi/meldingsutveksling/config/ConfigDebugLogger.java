package no.difi.meldingsutveksling.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * the onApplicationEvent is executed during startup of the application
 *
 * @Author Glenn Bech
 */
@Component
public class ConfigDebugLogger implements ApplicationListener<ContextRefreshedEvent> {

    private IntegrasjonspunktConfiguration config;

    @Autowired
    public ConfigDebugLogger(IntegrasjonspunktConfiguration config) {
        this.config = config;
    }

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        final ConfigMeta metadata = config.getMetadata();
        metadata.logInfo();
        System.out.println(metadata.toString());
    }

}