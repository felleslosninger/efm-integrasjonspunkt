package no.difi.meldingsutveksling.eventlog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventLogConfigurarion {
    @Autowired
    DataBaseConfig dbConfig;

    @Bean
    public EventLogDAO eventLogDAO() {
        return new EventLogDAO(dbConfig.getDataSource());
    }
}
