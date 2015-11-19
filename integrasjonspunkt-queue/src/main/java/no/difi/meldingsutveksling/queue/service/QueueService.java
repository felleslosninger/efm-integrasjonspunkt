package no.difi.meldingsutveksling.queue.service;

import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Date;

@Service
public class QueueService {
    protected static final String FILE_PATH = System.getProperty("user.dir") + "/queue/";
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");

    private final QueueDao queueDao;
    private PrivateKey privateKey;

    @Autowired
    public QueueService(QueueDao queueDao) {
        this.queueDao = queueDao;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
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
        return queueDao.retrieve(statusToGet).get(0);
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

        byte[] bytes = decryptMessage(buffer);


        return bytes;
    }

    /**
     * Used for new messages that is to be put on queue.
     *
     * @param request Request to be put on queue
     */
    public void put(Object request) {
        byte[] crypted = encryptMessage(request);
        String uniqueFilename = generateUniqueFileName();
        String filenameWithPath = ammendPath(uniqueFilename);

        saveFileOnDisk(crypted, filenameWithPath);

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

    private byte[] encryptMessage(Object request) {
        try {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            ObjectOutput output = new ObjectOutputStream(byteOutputStream);
            output.writeObject(request);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            return cipher.doFinal(byteOutputStream.toByteArray());

        } catch (NoSuchAlgorithmException | InvalidKeyException
                | NoSuchPaddingException | BadPaddingException
                | IllegalBlockSizeException | IOException e) {
            //TODO: Better logging
            e.printStackTrace();
        }
        return new byte[0];
    }

    private byte[] decryptMessage(Object request) {
        try {
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            ObjectOutput output = new ObjectOutputStream(byteOutputStream);
            output.writeObject(request);

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(byteOutputStream.toByteArray());

        } catch (NoSuchAlgorithmException | InvalidKeyException
                | NoSuchPaddingException | BadPaddingException
                | IllegalBlockSizeException | IOException e) {
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
}
