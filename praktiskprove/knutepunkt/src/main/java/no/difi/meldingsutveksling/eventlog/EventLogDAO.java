package no.difi.meldingsutveksling.eventlog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent stoarage for the Event Log
 *
 * @author Glenn Bech
 */

@Repository
public class EventLogDAO {

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    /**
     * Creates an event log entry
     *
     * @param e the Event
     */
    public void insertEventLog(Event e) {
        String insertSQL = "insert into EVENT_LOG(UUID, SENDER, RECEIVER, EVENT_TIMESTAMP, STATE, ERROR_MESSAGE) " +
                "values (:uuid, :sender, :receiver, :timestamp, :state, :errorMessage) ";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("uuid", e.getUuid().toString());
        params.put("sender", e.getSender());
        params.put("receiver", e.getReceiver());
        params.put("timestamp", e.getTimeStamp());
        params.put("state", e.getProcessState().toString());
        params.put("errorMessage", e.getExceptionMessage());
        jdbcTemplate.update(insertSQL, params);
    }

    /**
     * Get event log entries
     *
     * @param since the point in time to return log entries after
     */
    public List<Event> getEventLog(long since) {
        String q = "select * from EVENT_LOG where timestamp > :since";
        Map<String, Long> params = new HashMap<String, Long>();
        params.put("since", since);
        return jdbcTemplate.query(q, params, new RowMapper<Event>() {
            @Override
            public Event mapRow(ResultSet resultSet, int i) throws SQLException {
                Event e = new Event();
                e.setUuid(UUID.fromString(resultSet.getString("uuid")));
                e.setSender(resultSet.getString("sender"));
                e.setReceiver(resultSet.getString("receiver"));
                e.setTimeStamp(resultSet.getLong("event_timestamp"));
                e.setExceptionMessage(new Exception(resultSet.getString("error_message")));
                e.setProcessStates(ProcessState.valueOf(resultSet.getString("state")));
                return e;
            }
        });
    }


}
