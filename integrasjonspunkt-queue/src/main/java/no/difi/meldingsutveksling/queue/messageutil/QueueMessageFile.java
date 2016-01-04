package no.difi.meldingsutveksling.queue.messageutil;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import no.difi.meldingsutveksling.queue.domain.QueueElement;
import org.apache.commons.lang.RandomStringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystems;

public class QueueMessageFile {
    public static final String FILE_PATH = System.getProperty("user.dir") + "/queue/";

    public static boolean removeFile(String filename) {
        File file = new File(filename);
        return file.delete();
    }

    public static Object loadMessageFromFile(QueueElement queueElement) throws IOException {
        InputStream is = new FileInputStream(queueElement.getFileLocation());
        return new XStream(new DomDriver()).fromXML(is);
    }

    public static void saveFileOnDisk(Object encryptedMessage, String filename) throws IOException {
        ensureLocalDirectoryExists();
        String stringRepresentaiton = new XStream(new DomDriver()).toXML(encryptedMessage);
        BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
        br.write(stringRepresentaiton);
        br.close();
    }

    public static String ammendPath(String filename) {
        return FileSystems.getDefault().getPath(FILE_PATH, filename + ".queue").toString();
    }

    public static String generateUniqueFileName() {
        long millis = System.currentTimeMillis();
        String rndchars = RandomStringUtils.randomAlphanumeric(10);
        return rndchars + "_" + millis;
    }

    private static void ensureLocalDirectoryExists() throws IOException {
        File localDirectory = new File(FILE_PATH);
        if (!localDirectory.exists()) {
            boolean success = localDirectory.mkdir();
            if (!success) {
                throw new IOException("Can not create directory with name " + FILE_PATH);
            }
        }
    }
}
