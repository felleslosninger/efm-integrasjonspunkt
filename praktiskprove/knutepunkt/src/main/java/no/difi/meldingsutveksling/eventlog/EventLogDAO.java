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

    public EventLogDAO() {
    }

    public EventLogDAO(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

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
        String insertSQL = "insert into EVENT_LOG(UUID, SENDER, RECEIVER, EVENT_TIMESTAMP, STATE, ERROR_MESSAGE, MESSAGE, ARCHIVE_CONVERSATION_ID,HUB_CONVERSATION_ID,JPID) " +
                "values (:uuid, :sender, :receiver, :timestamp, :state, :errorMessage, :message, :arcCid, :hubCid, :jpid) ";
        Map<String, Object> params = new HashMap<>();
        params.put("uuid", e.getUuid().toString());
        params.put("sender", e.getSender());
        params.put("receiver", e.getReceiver());
        params.put("message", e.getMessage());
        params.put("timestamp", e.getTimeStamp());
        params.put("state", e.getProcessState().toString());
        params.put("errorMessage", e.getExceptionMessage());
        params.put("arcCid", e.getArkiveConversationId());
        params.put("hubCid", e.getHubConversationId());
        params.put("jpid", e.getJpId());
        jdbcTemplate.update(insertSQL, params);
    }

    /**
     * Get event log entries
     *
     * @param since the point in time to return log entries after
     */
    public List<Event> getEventLog(long since) {
        String q = "select * from EVENT_LOG where event_timestamp >= :since order by event_timestamp desc";
        Map<String, Long> params = new HashMap<>();
        params.put("since", since);
        return jdbcTemplate.query(q, params, new RowMapper<Event>() {
            @Override
            public Event mapRow(ResultSet resultSet, int i) throws SQLException {
                Event e = new Event(resultSet.getLong("event_timestamp"), UUID.fromString(resultSet.getString("uuid")));
                e.setSender(resultSet.getString("sender"));
                e.setReceiver(resultSet.getString("receiver"));
                e.setExceptionMessage(null);
                e.setMessage(resultSet.getString("message"));
                e.setProcessStates(ProcessState.valueOf(resultSet.getString("state")));
                return e;
            }
        });
    }

    public List<Event> getEventEntries(String id,String conversationType) {
        String q = "select * from EVENT_LOG where " + conversationType +" = " + id;

        return jdbcTemplate.query(q,new RowMapper<Event>() {
            @Override
            public Event mapRow(ResultSet resultSet, int i) throws SQLException {
                Event e = new Event(resultSet.getLong("event_timestamp"), UUID.fromString(resultSet.getString("uuid")));
                e.setSender(resultSet.getString("sender"));
                e.setReceiver(resultSet.getString("receiver"));
                e.setExceptionMessage(null);
                e.setMessage(resultSet.getString("message"));
                e.setProcessStates(ProcessState.valueOf(resultSet.getString("state")));
                e.setJpId(resultSet.getString("jpid"));
                e.setArkiveConversationId(resultSet.getString("arcCid"));
                e.setHubConversationId(resultSet.getString("hubCid"));
                return e;
            }
        } );
    }
}
