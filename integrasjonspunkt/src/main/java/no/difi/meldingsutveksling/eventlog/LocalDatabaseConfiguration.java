package no.difi.meldingsutveksling.eventlog;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@Configuration
@Profile("dev")
public class LocalDatabaseConfiguration implements DataBaseConfig {

    @Bean
    public javax.sql.DataSource getDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.DERBY)
                .setName("eventLogDB").continueOnError(true)
                .addScript("classpath:db/schema/ddl.sql")
                .build();
    }

}
