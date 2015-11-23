package no.difi.meldingsutveksling.queue.service;

import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;

@Service
@ManagedResource
public class QueueService {
    protected static final String FILE_PATH = System.getProperty("user.dir") + "/queue/";
    protected static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    private static final String BASE64_KEY = "ABEiM0RVZneImaq7zN3u/w==";
    private static final String BASE64_IV = "AAECAwQFBgcICQoLDA0ODw==";


    private final QueueDao queueDao;

    @Autowired
    public QueueService(QueueDao queueDao) {
        this.queueDao = queueDao;
    }

    /**
     * Used for existing messages when trying to resend failed messages or remove messages from queue that is sent.
     *
     * @param key Unique key for request to update
     */
    public void put(String key, int i) {
        //Krypter
        //Valider om ny melding
        //Lagre melding
        //Oppdater status
    }

    /**
     * Get metadata for first element
     *
     * @param statusToGet Type messages to check for in queue
     * @return Metadata for the top element to process
     */
    public Queue getNext(Status statusToGet) {
        //TODO: Only gets first for a certain status. Getting real next is not implemented.
        List<Queue> retrieve = queueDao.retrieve(statusToGet);
        if  (retrieve.size() > 0) {
            return retrieve.get(0);
        }
        else {
            return null;
        }
    }

    /***
     * Get message based on metadata
     *
     * @param unique id of the message to get
     * @return the original request ready to send
     */
    public Object getMessage(String unique) {
        Queue retrieve = queueDao.retrieve(unique);

        StringBuffer buffer = retrieveFileFromDisk(retrieve);

        return String.valueOf(buffer);

//        byte[] bytes = decryptMessage(String.valueOf(buffer));
//
//        return Arrays.toString(bytes);
    }

    /**
     * Used for new messages that is to be put on queue.
     *
     * @param request Request to be put on queue
     */
    public void put(String request) {
//        byte[] crypted = encryptMessage(request);
        String uniqueFilename = generateUniqueFileName();
        String filenameWithPath = ammendPath(uniqueFilename);

//        saveFileOnDisk(crypted, filenameWithPath);
        saveFileOnDisk(request.getBytes(), filenameWithPath);

        Queue newEntry = new Queue.Builder()
                .unique(uniqueFilename)
                .location(filenameWithPath)
                .status(Status.NEW)
                .numberAttempt(0)
                .rule(RuleDefault.getRule())
                .lastAttemptTime(new Date())
                .checksum(generateChecksum(filenameWithPath))
                .build();

        queueDao.saveEntry(newEntry);
    }

    public void success(String unique) {
        Queue queue = queueDao.retrieve(unique);
        removeFile(queue.getRequestLocation());

        int numberAttempts = queue.getNumberAttempts();

        Queue updatedQueue = queue.getOpenObjectBuilder()
                .status(Status.DONE)
                .lastAttemptTime(new Date())
                .location("")
                .numberAttempt(++numberAttempts)
                .build();

        queueDao.updateStatus(updatedQueue);
    }

    public void fail(String unique) {
        Queue queue = queueDao.retrieve(unique);

        int numberAttempts = queue.getNumberAttempts();
        Queue.Builder openObject = queue.getOpenObjectBuilder()
                .numberAttempt(++numberAttempts)
                .lastAttemptTime(new Date());

        if (numberAttempts > queue.getRule().getMaxAttempt()) {
            openObject.status(Status.ERROR);
            removeFile(queue.getRequestLocation());
        }
        else {
            openObject.status(Status.FAILED);
        }

        queueDao.saveEntry(openObject.build());
    }

    private StringBuffer retrieveFileFromDisk(Queue retrieve) {
        StringBuffer buffer = new StringBuffer();
        BufferedReader reader = null;

        try {
            String currentLine;
            reader = new BufferedReader(new FileReader(retrieve.getRequestLocation()));

            while ((currentLine = reader.readLine()) != null) {
                buffer.append(currentLine);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return buffer;
    }

    private void saveFileOnDisk(byte[] crypted, String filename) {
        if (!new File(FILE_PATH).exists()) {
            new File(FILE_PATH).mkdir();
        }

        File file = new File(filename);
        try {
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            BufferedWriter buffer = new BufferedWriter(writer);
            buffer.write(Arrays.toString(crypted));
            buffer.close();
        } catch (IOException e) {
            //Todo: Better logging
            e.printStackTrace();
        }
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

    private String ammendPath(String filename) {
        String fullFilename = FILE_PATH + filename + ".queue";

        if (IS_WINDOWS) {
            return fullFilename.replace("/", "\\");
        }
        return fullFilename;
    }

    private String generateUniqueFileName() {
        long millis = System.currentTimeMillis();
        String rndchars = RandomStringUtils.randomAlphanumeric(10);
        return rndchars + "_" + millis;
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

    private boolean removeFile(String filename) {
        File file = new File(filename);
        return file.delete();
    }

    @ManagedAttribute
    public int getQueueSize() {
        return queueDao.getQueueTotalSize();
    }

    @ManagedAttribute
    public int getQueueNewSize() {
        return queueDao.getQueueReadySize();
    }

    @ManagedAttribute
    public int getQueueFailedSize() {
        return queueDao.getQueueFailedSize();
    }

    @ManagedAttribute
    public int getQueueErrorSize() {
        return queueDao.getQueueErrorSize();
    }
    @ManagedAttribute
    public int getQueueDoneSize() {
        return queueDao.getQueueCompletedSize();
    }

}
