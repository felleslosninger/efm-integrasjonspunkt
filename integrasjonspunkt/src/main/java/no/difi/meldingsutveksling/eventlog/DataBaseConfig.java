package no.difi.meldingsutveksling.eventlog;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public interface DataBaseConfig {

    DataSource getDataSource();
}
