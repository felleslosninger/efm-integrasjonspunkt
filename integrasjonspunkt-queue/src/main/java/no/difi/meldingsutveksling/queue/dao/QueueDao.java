package no.difi.meldingsutveksling.queue.dao;

import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class QueueDao {
    @Autowired
    JdbcTemplate template;

    public void saveEntry(Queue queue) {
        Map<String, Object> params = new HashMap<>();
        params.put("unique_id", queue.getUnique());
        params.put("numberAttempt", queue.getNumberAttempts());
        params.put("rule", queue.getRule());
        params.put("status", queue.getStatus());
        params.put("requestLocation", queue.requestLocation());
        params.put("lastAttempt", queue.getLastAttemptTime());
        params.put("checksum", queue.getChecksum());

        String sql = "INSERT INTO queue_metadata " +
                "(unique_id, numberAttempt, rule, status, requestLocation, lastAttemptTime, checksum) " +
                "VALUES (:unique_id, :numberAttempt, :rule, :status, :requestLocation, :lastAttempt, :checksum)";

        template.update(sql, params);
    }

    public Queue retrieve(Status status) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", status.name());

        String sql = "SELECT * FROM queue_metadata " +
                "WHERE status=:status";

        return template.queryForObject(sql, new QueueMapper(), params);

    }

    private static final class QueueMapper implements RowMapper<Queue> {

        public Queue mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Queue.Builder()
                    .unique(rs.getString("unique_id"))
                    .numberAttempt(rs.getInt("numberAttempt"))
                    .rule((Rule) rs.getObject("rule"))
                    .status(rs.getString("status"))
                    .location(rs.getString("requestLocation"))
                    .lastAttemptTime(rs.getTimestamp("lastAttempt"))
                    .checksum(rs.getString("checksum"))
                    .build();
        }
    }
}
