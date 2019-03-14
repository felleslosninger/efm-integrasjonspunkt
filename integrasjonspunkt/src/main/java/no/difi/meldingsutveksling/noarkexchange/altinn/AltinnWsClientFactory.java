package no.difi.meldingsutveksling.noarkexchange.altinn;

import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsConfiguration;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class AltinnWsClientFactory implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public AltinnWsClient getAltinnWsClient(ServiceRecord serviceRecord) {
        AltinnWsConfiguration configuration = AltinnWsConfiguration.fromConfiguration(serviceRecord, applicationContext);
        return new AltinnWsClient(configuration, applicationContext);
    }
}
