package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktImpl;
import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.service.QueueService;
import no.difi.meldingsutveksling.scheduler.QueueScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntegrasjonspunktBeans {
    @Bean
    public QueueScheduler queueScheduler(IntegrasjonspunktImpl integrasjonspunkt) {
        return new QueueScheduler(queueService(), integrasjonspunkt);
    }

    @Bean
    public QueueService queueService() {
        return new QueueService(new QueueDao());
    }

    @Bean
    public IntegrasjonspunktImpl integrasjonspunktImpl() {
        return new IntegrasjonspunktImpl();
    }
}
