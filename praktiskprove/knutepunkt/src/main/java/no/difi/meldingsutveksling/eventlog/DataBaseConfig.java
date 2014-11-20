package no.difi.meldingsutveksling.eventlog;

import javax.sql.DataSource;

public interface DataBaseConfig {

    DataSource getDataSource();
}
