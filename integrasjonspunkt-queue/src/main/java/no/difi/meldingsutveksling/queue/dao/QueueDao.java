package no.difi.meldingsutveksling.queue.dao;

import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCountCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@Repository
public class QueueDao {
    public static final int ONE_MINUTE_IN_MILLIS = 60000;

    @Autowired
    @Qualifier("queueJDBCTemplate")
    private JdbcTemplate template;

    @Autowired
    @Qualifier("queueJDBCNamedTemplate")
    private NamedParameterJdbcTemplate namedTemplate;

    public void saveEntry(Queue queue) {
        String sql = "INSERT INTO queue_metadata "
                + "(unique_id, numberAttempt, rule, status, requestLocation, lastAttemptTime, checksum) "
                + "VALUES (:uniqueId, :numberAttempts, :ruleName, :statusName, :fileLocation, :lastAttemptTime, :checksum)";

        SqlParameterSource params = new BeanPropertySqlParameterSource(queue);

        namedTemplate.update(sql, params);
    }

    public void updateEntry(Queue queue) {
        String sql = "UPDATE queue_metadata "
                + "SET numberAttempt = :numberAttempts, "
                + "rule = :ruleName, "
                + "status = :statusName, "
                + "requestLocation = :fileLocation, "
                + "lastAttemptTime = :lastAttemptTime, "
                + "checksum = :checksum "
                + "WHERE unique_id = :uniqueId ";

        SqlParameterSource params = new BeanPropertySqlParameterSource(queue);

        namedTemplate.update(sql, params);
    }

    public List<Queue> retrieve(Status status) {
        String sql = "SELECT unique_id, numberAttempt, rule, status, requestLocation, lastAttemptTime, checksum "
                + "FROM queue_metadata "
                + "WHERE status = :statusName ";

        List<Queue> unfilteredQueue = QueueMapper.map(template.queryForList(sql, status.name()));
        List<Queue> queueList = filterQueue(unfilteredQueue);

        sortQueue(queueList);

        return queueList;
    }

    public static Date addMinutesToDate(Date beforeTime, int minutes) {
        final long ONE_MINUTE_IN_MILLIS = 60000;

        long curTimeInMs = beforeTime.getTime();
        return new Date(curTimeInMs + (minutes * ONE_MINUTE_IN_MILLIS));
    }

    public void updateStatus(Queue queue) {
        String sql = "UPDATE queue_metadata "
                + "SET status = :statusName, "
                + "lastAttemptTime = :lastAttemptTime, "
                + "numberAttempt = :numberAttempts "
                + "WHERE unique_id = :uniqueId";

        SqlParameterSource params = new BeanPropertySqlParameterSource(queue);

        namedTemplate.update(sql, params);
    }

    protected void removeAll() {
        String sql = "DELETE FROM queue_metadata ";

        template.execute(sql);
    }

    private void sortQueue(List<Queue> queueList) {
        Collections.sort(queueList, new Comparator<Queue>() {
            public int compare(Queue o1, Queue o2) {
                return o1.getLastAttemptTime().compareTo(o2.getLastAttemptTime());
            }
        });
    }

    private List<Queue> filterQueue(List<Queue> queueList) {
        List<Queue> queueToReturn = new ArrayList<>();

        Date now = new Date();
        for (Queue queue : queueList) {
            int minDelay = queue.getRule().getMinutesToNextAttempt(queue.getNumberAttempts());
            Date lastAttempt = queue.getLastAttemptTime();
            Date newAttemptAfter = addMinutesToDate(lastAttempt, minDelay);

            if (now.getTime() > newAttemptAfter.getTime()) {
                queueToReturn.add(queue);
            }
        }

        return queueToReturn;
    }

    public Queue retrieve(String uniqueId) {
        String sql = "SELECT unique_id, numberAttempt, rule, status, requestLocation, lastAttemptTime, checksum "
                + "FROM queue_metadata "
                + "WHERE unique_id = :uniqueId ";

        List<Queue> queue = QueueMapper.map(template.queryForList(sql, uniqueId));
        return queue.get(0);
    }

    private static final class QueueMapper implements RowMapper<Queue> {

        public Queue mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Queue.Builder()
                    .uniqueId(rs.getString("unique_id"))
                    .numberAttempt(rs.getInt("numberAttempt"))
                    .rule((Rule) rs.getObject("rule"))
                    .status(rs.getString("status"))
                    .location(rs.getString("requestLocation"))
                    .lastAttemptTime(rs.getTimestamp("lastAttemptTime"))
                    .checksum(rs.getString("checksum"))
                    .build();
        }

        public static List<Queue> map(List<Map<String, Object>> maps) {
            ArrayList<Queue> queues = new ArrayList<>();

            for (Map map : maps) {
                queues.add(new Queue.Builder()
                        .uniqueId(map.get("unique_id").toString())
                        .numberAttempt(Integer.parseInt(map.get("numberAttempt").toString()))
                        .rule(map.get("rule").toString())
                        .status(Status.valueOf(map.get("status").toString()))
                        .location(map.get("requestLocation").toString()).lastAttemptTime((Date) map.get("lastAttemptTime"))
                        .checksum(map.get("checksum").toString())
                        .build());
            }
            return queues;
        }
    }

    public int getQueueSize() {
        RowCountCallbackHandler countCallback = new RowCountCallbackHandler();
        template.query("select * from queue_metadata", countCallback);
        return countCallback.getRowCount();
    }

    public int getQueueSize(Status status) {
        String sql = format("select * from queue_metadata where status = '%s'", status.name());
        RowCountCallbackHandler countCallback = new RowCountCallbackHandler();
        template.query(sql, countCallback);
        return countCallback.getRowCount();
    }
}
