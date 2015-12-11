package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRequiredPropertyException;
import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktImpl;
import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.service.Queue;
import no.difi.meldingsutveksling.scheduler.QueueScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class IntegrasjonspunktBeans {
    @Autowired
    private Environment environment;

    @Bean
    public QueueScheduler queueScheduler(IntegrasjonspunktImpl integrasjonspunkt, IntegrasjonspunktConfig config) {
        return new QueueScheduler(queueService(), integrasjonspunkt, config);
    }

    @Bean
    public Queue queueService() {
        return new Queue(new QueueDao());
    }

    @Bean
    public IntegrasjonspunktConfiguration integrasjonspunktConfiguration() throws MeldingsUtvekslingRequiredPropertyException {
        return new IntegrasjonspunktConfiguration(environment);
    }
}
