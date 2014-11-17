package no.difi.meldingsutveksling.eventlog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

/**
 */

@Repository
public class EventLogDAO {

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

    }

    public void insertEventLog(Event e) {
        String sql = "select count(*) from T_ACTOR where first_name = :first_name";
        Map<String, String> namedParameters = Collections.singletonMap("first_name", "2");
    }


}
