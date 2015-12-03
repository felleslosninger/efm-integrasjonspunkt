package no.difi.meldingsutveksling.queue.config;

import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.service.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@ComponentScan({"no.difi.meldingsutveksling.queue"})
@Configuration
public class QueueConfig {
    @Bean
    public Queue queueService() {
        return new Queue(queueDao());
    }

    @Bean
    public QueueDao queueDao() {
        return new QueueDao();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
        return builder
                .setName("queue")
                .setType(EmbeddedDatabaseType.HSQL)
                .addScript("queuedb/create-db.sql")
                .build();
    }

    @Bean(name = "queueJDBCTemplate")
    public JdbcTemplate queueJDBCTemplate( DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "queueJDBCNamedTemplate")
    public NamedParameterJdbcTemplate queueJDBCNamedTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
