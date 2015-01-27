package no.difi.meldingsutveksling.eventlog;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.URI;
import java.net.URISyntaxException;


@Configuration
@Profile("heroku")
public class HerokuDatabaseConfig implements DataBaseConfig {

    public static final String DATABASE_URL_ENV_PARAM = "DATABASE_URL";

    @Bean
    public BasicDataSource getDataSource() {
        URI dbUri = null;
        try {
            dbUri = new URI(System.getenv(DATABASE_URL_ENV_PARAM));
        } catch (URISyntaxException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setUrl(dbUrl);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);
        return basicDataSource;
    }
}
