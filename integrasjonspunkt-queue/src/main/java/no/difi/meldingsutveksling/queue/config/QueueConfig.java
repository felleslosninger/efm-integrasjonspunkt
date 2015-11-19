package no.difi.meldingsutveksling.queue.config;

import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.service.QueueService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@ComponentScan({"no.difi.meldingsutveksling.queue"})
@Configuration
public class QueueConfig {
    @Bean
    public QueueService queueService() {
        return new QueueService(queueDao());
    }

    @Bean
    public QueueDao queueDao() {
        return new QueueDao();
    }

    @Bean
    public DataSource dataSource() {
        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        return builder
                .setName("queue")
                .setType(EmbeddedDatabaseType.HSQL)
                .addScript("queuedb/create-db.sql")
                .build();
    }

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource());
    }
}
