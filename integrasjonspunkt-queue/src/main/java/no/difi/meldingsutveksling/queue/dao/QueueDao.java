package no.difi.meldingsutveksling.queue.dao;

import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public class QueueDao {
    @Autowired
    private JdbcTemplate template;

    public void saveEntry(Queue queue) {
        String sql = "INSERT INTO queue_metadata "
                + "(unique_id, numberAttempt, rule, status, requestLocation, lastAttemptTime, checksum) "
                + "VALUES (:unique_id, :numberAttempt, :rule, :status, :requestLocation, :lastAttempt, :checksum)";

        template.update(sql, queue.getUnique(), queue.getNumberAttempts(), queue.getRuleName(), queue.getStatus(),
                queue.getRequestLocation(), queue.getLastAttemptTime(), queue.getChecksum());
    }

    public List<Queue> retrieve(Status status) {
        String sql = "SELECT unique_id, numberAttempt, rule, status, requestLocation, lastAttemptTime, checksum "
                + "FROM queue_metadata "
                + "WHERE status = :status ";

        List<Queue> queue = QueueMapper.map(template.queryForList(sql, status));

        Collections.sort(queue, new Comparator<Queue>() {
            public int compare(Queue o1, Queue o2) {
                return o1.getLastAttemptTime().compareTo(o2.getLastAttemptTime());
            }
        });

        return queue;
    }

    public void updateStatus(Queue object) {
        String sql = "UPDATE queue_metadata "
                + "SET status = :status, "
                + "lastAttemptTime = :lastAttemptTime, "
                + "numberAttempt = :numberAttempt "
                + "WHERE unique_id = :unique ";

        template.update(sql, object.getStatus(), object.getLastAttemptTime(), object.getNumberAttempts(), object.getUnique());
    }

    protected void removeAll() {
        String sql = "DELETE FROM queue_metadata ";

        template.execute(sql);
    }

    public Queue retrieve(String unique) {
        String sql = "SELECT * FROM queue_metadata WHERE unique_id = :unique ";

        return QueueMapper.map(template.queryForList(sql, unique)).get(0);
    }

    private static final class QueueMapper implements RowMapper<Queue> {

        public Queue mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Queue.Builder()
                    .unique(rs.getString("unique_id"))
                    .numberAttempt(rs.getInt("numberAttempt"))
                    .rule((Rule) rs.getObject("rule"))
                    .status(rs.getString("status"))
                    .location(rs.getString("requestLocation"))
                    .lastAttemptTime(rs.getTimestamp("lastAttemptTime"))
                    .checksum(rs.getString("checksum"))
                    .build();
        }

        public static List<Queue> map(List<Map<String, Object>> maps) {
            ArrayList<Queue> queueList = new ArrayList<>();

            for (Map map : maps) {
                queueList.add(new Queue.Builder().unique(map.get("unique_id").toString()).numberAttempt(Integer.parseInt(map.get("numberAttempt").toString())).rule(map.get("rule").toString()).status(Status.statusFromString(map.get("status").toString())).location(map.get("requestLocation").toString()).lastAttemptTime((Date) map.get("lastAttemptTime")).checksum(map.get("checksum").toString()).build());
            }
            return queueList;
        }
    }
}
