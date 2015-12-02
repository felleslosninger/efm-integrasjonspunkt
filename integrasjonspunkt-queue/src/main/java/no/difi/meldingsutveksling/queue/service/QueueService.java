package no.difi.meldingsutveksling.queue.service;

import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.messageutil.QueueMessageFile;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;

@Service
@ManagedResource
public class QueueService {
    private static final String BASE64_KEY = "ABEiM0RVZneImaq7zN3u/w==";
    private static final String BASE64_IV = "AAECAwQFBgcICQoLDA0ODw==";

    private final QueueDao queueDao;

    @Autowired
    public QueueService(QueueDao queueDao) {
        this.queueDao = queueDao;
    }

    /**
     * Get metadata for first element
     *
     * @return Metadata for the top element to process
     */
    public Queue getNext() {
        List<Queue> retrieveRetry = queueDao.retrieve(Status.RETRY);
        if  (retrieveRetry.size() > 0) {
            return retrieveRetry.get(0);
        }
        else {
            List<Queue> retrieveNew = queueDao.retrieve(Status.NEW);
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
        Queue retrieve = queueDao.retrieve(uniqueId);

        return QueueMessageFile.loadMessageFromFile(retrieve);

//        byte[] bytes = decryptMessage(String.valueOf(buffer));
//
//        return Arrays.toString(bytes);
    }

    /**
     * Used for new messages that is to be put on queue.
     *
     * @param request Request to be put on queue
     */
    public void put(Object request) throws IOException {


//        byte[] crypted = encryptMessage(request);
        String uniqueFilename = QueueMessageFile.generateUniqueFileName();
        String filenameWithPath = QueueMessageFile.ammendPath(uniqueFilename);

//        saveFileOnDisk(crypted, filenameWithPath);
        QueueMessageFile.saveFileOnDisk(request, filenameWithPath);

        Queue newEntry = new Queue.Builder()
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
        Queue queue = queueDao.retrieve(uniqueId);
        QueueMessageFile.removeFile(queue.getFileLocation());

        int numberAttempts = queue.getNumberAttempts();

        Queue updatedQueue = queue.getOpenObjectBuilder()
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
        Queue queue = queueDao.retrieve(uniqueId);

        int numberAttempts = queue.getNumberAttempts();
        Queue.Builder openObject = queue.getOpenObjectBuilder()
                .numberAttempt(++numberAttempts)
                .lastAttemptTime(new Date());

        if (numberAttempts > queue.getRule().getMaxAttempt()) {
            openObject.status(Status.ERROR);
            QueueMessageFile.removeFile(queue.getFileLocation());
        }
        else {
            openObject.status(Status.RETRY);
        }

        Queue builtObject = openObject.build();
        queueDao.updateEntry(builtObject);

        return builtObject.getStatus();
    }

    private byte[] encryptMessage(String request) {
        try {
            return AES.encrypt(parseBase64Binary(BASE64_KEY), parseBase64Binary(BASE64_IV), parseBase64Binary(request));
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            //TODO: Better logging
            e.printStackTrace();
        }
        return new byte[0];
    }

    private byte[] decryptMessage(String request) {
        try {
            return AES.decrypt(parseBase64Binary(BASE64_KEY), parseBase64Binary(BASE64_IV), parseBase64Binary(request));

        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            //TODO: Better logging
            e.printStackTrace();
        }
        return new byte[0];
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
            //TODO: Better logging
            e.printStackTrace();
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
