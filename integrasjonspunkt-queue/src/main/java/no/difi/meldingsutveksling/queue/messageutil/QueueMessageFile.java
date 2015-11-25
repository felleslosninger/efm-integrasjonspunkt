package no.difi.meldingsutveksling.queue.messageutil;

import no.difi.meldingsutveksling.queue.domain.Queue;
import org.apache.commons.lang.RandomStringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class QueueMessageFile {
    public static final String FILE_PATH = System.getProperty("user.dir") + "/queue/";
    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");

    public static boolean removeFile(String filename) {
        File file = new File(filename);
        return file.delete();
    }

    public static StringBuffer retrieveFileFromDisk(Queue retrieve) {
        FileInputStream inputStream;
        StringBuffer output = new StringBuffer();
        try {
            inputStream = new FileInputStream(retrieve.getRequestLocation());
            System.out.println("Total file size read (in bytes  ) : " + inputStream.available());

            int content;
            while ((content = inputStream.read()) != -1) {
                output.append((char)content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output;
    }

    public static void saveFileOnDisk(byte[] crypted, String filename) {
        if (!new File(FILE_PATH).exists()) {
            new File(FILE_PATH).mkdir();
        }

        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(filename);
            outputStream.write(crypted);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
