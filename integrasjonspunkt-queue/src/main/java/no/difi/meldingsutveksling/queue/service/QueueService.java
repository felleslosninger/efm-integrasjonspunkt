package no.difi.meldingsutveksling.queue.service;

import no.difi.meldingsutveksling.queue.dao.QueueDao;
import no.difi.meldingsutveksling.queue.domain.Queue;
import no.difi.meldingsutveksling.queue.domain.Status;
import no.difi.meldingsutveksling.queue.rule.Rule;
import no.difi.meldingsutveksling.queue.rule.RuleDefault;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
    private static final String FILE_PATH = System.getProperty("user.dir") + "/queue/";
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");

    private final Rule rule;
    private final PrivateKey privateKey;

    @Autowired
    QueueDao queueDao;

    public QueueService(Rule rule, PrivateKey privateKey) {
        this.rule = rule;
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
     * Get messages with a certain status.
     *
     * @param statusToGet Type messages to check for in queue
     */
    public void get(Status statusToGet) {
        //Oppdater status (aktiv melding)
        //Dekrypter
        //returner melding
    }

    /**
     * Used for new messages that is to be put on queue.
     *
     * @param request Request to be put on queue
     */
    public void put(Object request) {
        byte[] crypted = cryptMessage(request);
        String uniqueFilename = generateUniqueFileName();
        String filenameWithPath = ammendPath(uniqueFilename);

        saveFileOnDisk(crypted, uniqueFilename);

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

    private void saveFileOnDisk(byte[] crypted, String filename) {
        File file = new File(filename);
        try {
            FileWriter writer = new FileWriter(file.getAbsoluteFile());
            BufferedWriter buffer = new BufferedWriter(writer);
            buffer.write(Arrays.toString(crypted));
            buffer.close();
        } catch (IOException e) {
            //Todo: Better logging
            e.printStackTrace();
        }
    }

    private byte[] cryptMessage(Object request) {
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

    private String ammendPath(String filename) {
        String fullFilename = FILE_PATH + filename + ".file";

        if (IS_WINDOWS) {
            return fullFilename.replace("/", "\\");
        }
        return fullFilename;
    }

    private String generateUniqueFileName() {
        long millis = System.currentTimeMillis();
        String datetime = new Date().toString();
        datetime = datetime.replace(" ", "");
        datetime = datetime.replace(":", "");
        String rndchars = RandomStringUtils.randomAlphanumeric(16);
        return rndchars + "_" + datetime + "_" + millis;
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
