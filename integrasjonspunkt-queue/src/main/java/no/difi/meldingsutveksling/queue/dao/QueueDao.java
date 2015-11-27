package no.difi.meldingsutveksling.queue.dao;

import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCountCallbackHandler;
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
    public static final int ONE_MINUTE_IN_MILLIS = 60000;

    @Autowired
    @Qualifier("queueJDBCTemplate")
    private JdbcTemplate template;

    public void saveEntry(Queue queue) {
        String sql = "INSERT INTO queue_metadata "
                + "(unique_id, numberAttempt, rule, status, requestLocation, lastAttemptTime, checksum) "
                + "VALUES (:unique_id, :numberAttempt, :rule, :status, :requestLocation, :lastAttempt, :checksum)";

        template.update(sql, queue.getUnique(), queue.getNumberAttempts(), queue.getRuleName(),
                queue.getStatus().name(), queue.getFileLocation(), queue.getLastAttemptTime(), queue.getChecksum());
    }

    public void updateEntry(Queue queue) {
        String sql = "UPDATE queue_metadata "
                + "SET numberAttempt = :numberAttempt, "
                + "rule = :rule, "
                + "status = :status, "
                + "requestLocation = :requestLocation, "
                + "lastAttemptTime = :lastAttemptTime, "
                + "checksum = :checksum "
                + "WHERE unique_id = :uniqueId ";

        template.update(sql, queue.getNumberAttempts(), queue.getRuleName(), queue.getStatus().name(), queue.getFileLocation(), queue.getLastAttemptTime(), queue.getChecksum(), queue.getUnique());
    }

    public List<Queue> retrieve(Status status) {
        String sql = "SELECT unique_id, numberAttempt, rule, status, requestLocation, lastAttemptTime, checksum "
                + "FROM queue_metadata "
                + "WHERE status = :status ";

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

    public void updateStatus(Queue object) {
        String sql = "UPDATE queue_metadata "
                + "SET status = :status, "
                + "lastAttemptTime = :lastAttemptTime, "
                + "numberAttempt = :numberAttempt "
                + "WHERE unique_id = :uniqueId";

        template.update(sql, object.getStatus().name(), object.getLastAttemptTime(), object.getNumberAttempts(),
                object.getUnique());
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

        for (Queue queue : queueList) {
            int minDelay = queue.getRule().getInterval(queue.getNumberAttempts());
            Date now = new Date();
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

    public int getQueueTotalSize() {
        RowCountCallbackHandler countCallback = new RowCountCallbackHandler();
        template.query("select * from queue_metadata", countCallback);
        return countCallback.getRowCount();
    }

    public int getQueueReadySize() {
        RowCountCallbackHandler countCallback = new RowCountCallbackHandler();
        template.query("select * from queue_metadata where status = 'NEW'", countCallback);
        return countCallback.getRowCount();
    }

    public int getQueueFailedSize() {
        RowCountCallbackHandler countCallback = new RowCountCallbackHandler();
        template.query("select * from queue_metadata where status = 'RETRY'", countCallback);
        return countCallback.getRowCount();
    }

    public int getQueueErrorSize() {
        RowCountCallbackHandler countCallback = new RowCountCallbackHandler();
        template.query("select * from queue_metadata where status = 'ERROR'", countCallback);
        return countCallback.getRowCount();
    }

    public int getQueueCompletedSize() {
        RowCountCallbackHandler countCallback = new RowCountCallbackHandler();
        template.query("select * from queue_metadata where status = 'DONE'", countCallback);
        return countCallback.getRowCount();
    }
}
