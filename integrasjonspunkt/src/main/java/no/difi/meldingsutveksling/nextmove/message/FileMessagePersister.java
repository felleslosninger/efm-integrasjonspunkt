package no.difi.meldingsutveksling.nextmove.message;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.api.MessagePersister;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.persistence.PersistenceException;
import java.io.*;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.nextmove.useDbPersistence", havingValue = "false")
public class FileMessagePersister implements MessagePersister {

    private IntegrasjonspunktProperties props;

    @Autowired
    public FileMessagePersister(IntegrasjonspunktProperties props) {
        this.props = props;
    }

    @Override
    public void write(String messageId, String filename, byte[] message) throws IOException {
        String filedir = getMessageFiledirPath(messageId);
        File localFile = new File(filedir + filename);
        localFile.getParentFile().mkdirs();

        try (FileOutputStream os = new FileOutputStream(localFile);
             BufferedOutputStream bos = new BufferedOutputStream(os)) {
            bos.write(message);
        } catch (IOException e) {
            log.error("Could not write asic container to disk.", e);
            throw e;
        }
    }

    @Override
    public void writeStream(String messageId, String filename, InputStream inputStream, long size) throws IOException {
        String filedir = getMessageFiledirPath(messageId);
        File localFile = new File(filedir + filename);
        localFile.getParentFile().mkdirs();

        try (FileOutputStream os = new FileOutputStream(localFile);
             BufferedOutputStream bos = new BufferedOutputStream(os)) {
            int bytes = IOUtils.copy(inputStream, bos);
            bos.flush();
            log.debug("Storing {} for message[id={}]: {}", filename, messageId, humanReadableByteCount(bytes));
        } catch (IOException e) {
            log.error("Could not write asic container to disk.", e);
            throw e;
        }
    }

    private String humanReadableByteCount(int bytes) {
        int unit = 1000;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "kMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    @Override
    public byte[] read(String messageId, String filename) throws IOException {
        String filedir = getMessageFiledirPath(messageId);
        File file = new File(filedir + filename);
        return FileUtils.readFileToByteArray(file);
    }

    @Override
    public FileEntryStream readStream(String messageId, String filename) {
        String filedir = getMessageFiledirPath(messageId);
        File file = new File(filedir + filename);
        log.debug("Reading stream for file: {}", file.getAbsolutePath());
        try {
            BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
            return FileEntryStream.of(fis, file.length());
        } catch (FileNotFoundException e) {
            throw new PersistenceException(String.format("File \"%s\" not found for messageId \"%s\"", filename, messageId));
        }
    }

    @Override
    public void delete(String messageId) throws IOException {
        File dir = new File(getMessageFiledirPath(messageId));
        log.debug("Deleting directory {} for message[id={}]", dir.getAbsolutePath(), messageId);
        FileUtils.deleteDirectory(dir);
    }

    private String getMessageFiledirPath(String messageId) {
        String filedir = props.getNextmove().getFiledir();
        if (!filedir.endsWith("/")) {
            filedir = filedir + "/";
        }
        return filedir + messageId + "/";
    }
}
