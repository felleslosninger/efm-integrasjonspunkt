package no.difi.meldingsutveksling.queue.service;

import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.domain.QueueElement;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.exception.QueueException;
import no.difi.meldingsutveksling.queue.messageutil.QueueMessageFile;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

@ManagedResource
public class Queue {
    private final QueueDao queueDao;

    @Autowired
    public Queue(QueueDao queueDao) {
        this.queueDao = queueDao;
    }

    /**
     * Get metadata for first element
     *
     * @return Metadata for the top element to process
     */
    public QueueElement getNext() {
        List<QueueElement> retrieveRetry = queueDao.retrieve(Status.RETRY);
        if  (retrieveRetry.size() > 0) {
            return retrieveRetry.get(0);
        }
        else {
            List<QueueElement> retrieveNew = queueDao.retrieve(Status.NEW);
            if (retrieveNew.size() > 0) {
                return retrieveNew.get(0);
            }
            else {
                return null;
            }
        }
    }

    /***
     * Get message based on metadata
     *
     * @param uniqueId id of the message to get
     * @return the original request ready to send
     */
    public Object getMessage(String uniqueId) throws IOException {
        QueueElement retrieve = queueDao.retrieve(uniqueId);

        return QueueMessageFile.loadMessageFromFile(retrieve);
    }

    /**
     * Used for new messages that is to be put on queue.
     *
     * @param request Request to be put on queue
     */
    public void put(Object request) throws IOException {
        String uniqueFilename = QueueMessageFile.generateUniqueFileName();
        String filenameWithPath = QueueMessageFile.ammendPath(uniqueFilename);

        QueueMessageFile.saveFileOnDisk(request, filenameWithPath);

        QueueElement newEntry = new QueueElement.Builder()
                .uniqueId(uniqueFilename)
                .location(filenameWithPath)
                .status(Status.NEW)
                .numberAttempt(0)
                .rule(RuleDefault.getRule())
                .lastAttemptTime(new Date())
                .checksum(generateChecksum(filenameWithPath))
                .build();

        queueDao.saveEntry(newEntry);
    }

    /***
     * This method should be called when a message have successfully been processed.
     * It will clean up the message on disk, and update meta-data to reflect successfully sent message.
     *
     * @param uniqueId unique id for the queue element
     */
    public void success(String uniqueId) {
        QueueElement queueElement = queueDao.retrieve(uniqueId);
        QueueMessageFile.removeFile(queueElement.getFileLocation());

        int numberAttempts = queueElement.getNumberAttempts();

        QueueElement updatedQueue = queueElement.getOpenObjectBuilder()
                .status(Status.DONE)
                .lastAttemptTime(new Date())
                .location("")
                .numberAttempt(++numberAttempts)
                .build();

        queueDao.updateStatus(updatedQueue);
    }

    /***
     * When an item that have been picked up by the queue fails, this method should be called.
     * It contains logic for retries and updating meta-data for the element, and will return if it is a fail
     * for retry or a permanent error.
     *
     * @param uniqueId unique message id
     * @return Status.RETRY if it should be tried again at a later time, Status.ERROR if it is a permanent error.
     */
    public Status fail(String uniqueId) {
        QueueElement queueElement = queueDao.retrieve(uniqueId);

        int numberAttempts = queueElement.getNumberAttempts();
        QueueElement.Builder openObject = queueElement.getOpenObjectBuilder()
                .numberAttempt(++numberAttempts)
                .lastAttemptTime(new Date());

        if (numberAttempts > queueElement.getRule().getMaxAttempt()) {
            openObject.status(Status.ERROR);
            QueueMessageFile.removeFile(queueElement.getFileLocation());
        }
        else {
            openObject.status(Status.RETRY);
        }

        QueueElement builtObject = openObject.build();
        queueDao.updateEntry(builtObject);

        return builtObject.getStatus();
    }

    private String generateChecksum(String filenameWithPath) {
        StringBuilder sb = new StringBuilder("");
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            FileInputStream fileInput = new FileInputStream(filenameWithPath);
            byte[] dataBytes = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInput.read(dataBytes)) != -1) {
                messageDigest.update(dataBytes, 0, bytesRead);
            }
            byte[] digestBytes = messageDigest.digest();
            for (byte digestByte : digestBytes) {
                sb.append(Integer.toString((digestByte & 0xff) + 0x100, 16).substring(1));
            }

            fileInput.close();

        } catch (NoSuchAlgorithmException | IOException e) {
            throw new QueueException("Error while creating checksum", e);
        }
        return sb.toString();
    }

    @ManagedAttribute
    public int getQueueSize() {
        return queueDao.getQueueSize();
    }

    @ManagedAttribute
    public int getQueueNewSize() {
        return queueDao.getQueueSize(Status.NEW);
    }

    @ManagedAttribute
    public int getQueueRetrySize() {
        return queueDao.getQueueSize(Status.RETRY);
    }

    @ManagedAttribute
    public int getQueueErrorSize() {
        return queueDao.getQueueSize(Status.ERROR);
    }

    @ManagedAttribute
    public int getQueueDoneSize() {
        return queueDao.getQueueSize(Status.DONE);
    }
}
