package no.difi.meldingsutveksling.eventlog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * A Spring bean that is responsible for creating the database on startup of the spring context
 *
 * @author Glenn Bech
 */
public class CreateDB {

    @Autowired
    private DataSource dataSource;

    public void createDatabase() {
        JdbcTemplate t = new JdbcTemplate(dataSource);
        String ddl = "CREATE TABLE EVENT_LOG (" +
                "UUID VARCHAR(36)," +
                "SENDER VARCHAR(9)," +
                "RECEIVER VARCHAR(9)," +
                "EVENT_TIMESTAMP INT," +
                "STATE VARCHAR(50)," +
                "ERROR_MESSAGE  LONG VARCHAR," +
                "PRIMARY KEY(UUID))";
        System.out.println(ddl);
        t.update(ddl);


        t.update("CREATE index I_EVENT_LOG_TIMESTAMP ON EVENT_LOG(EVENT_TIMESTAMP)");
    }

}
