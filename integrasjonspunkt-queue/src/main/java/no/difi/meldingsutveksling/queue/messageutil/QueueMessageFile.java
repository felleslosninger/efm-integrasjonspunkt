package no.difi.meldingsutveksling.queue.messageutil;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import no.difi.meldingsutveksling.queue.domain.Queue;
import org.apache.commons.lang.RandomStringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

public class QueueMessageFile {
    public static final String FILE_PATH = System.getProperty("user.dir") + "/queue/";
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");

    public static boolean removeFile(String filename) {
        File file = new File(filename);
        return file.delete();
    }

    public static Object retrieveFileFromDisk(Queue queueElement) throws IOException {
        InputStream is = new FileInputStream(queueElement.getFileLocation());
        return new XStream(new DomDriver()).fromXML(is);
    }

    public static void saveFileOnDisk(Object crypted, String filename) throws IOException {
        if (!new File(FILE_PATH).exists()) {
            new File(FILE_PATH).mkdir();
        }
        String stringRepresentaiton = new XStream(new DomDriver()).toXML(crypted);
        BufferedWriter br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
        br.write(stringRepresentaiton);
        br.close();
    }

    public static String ammendPath(String filename) {
        String fullFilename = FILE_PATH + filename + ".queue";

        if (IS_WINDOWS) {
            return fullFilename.replace("/", "\\");
        }
        return fullFilename;
    }

    public static String generateUniqueFileName() {
        long millis = System.currentTimeMillis();
        String rndchars = RandomStringUtils.randomAlphanumeric(10);
        return rndchars + "_" + millis;
    }
}
