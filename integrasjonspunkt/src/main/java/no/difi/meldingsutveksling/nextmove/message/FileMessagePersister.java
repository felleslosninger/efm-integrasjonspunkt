package no.difi.meldingsutveksling.nextmove.message;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.persistence.PersistenceException;
import java.io.*;

import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;

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
    public void write(String conversationId, String filename, byte[] message) throws IOException {
        String filedir = getConversationFiledirPath(conversationId);
        File localFile = new File(filedir+filename);
        localFile.getParentFile().mkdirs();

        if (props.getNextmove().getApplyZipHeaderPatch() && ASIC_FILE.equals(filename)){
            BugFix610.applyPatch(message, conversationId);
        }

        try (FileOutputStream os = new FileOutputStream(localFile);
             BufferedOutputStream bos = new BufferedOutputStream(os)) {
            bos.write(message);
        } catch (IOException e) {
            log.error("Could not write asic container to disk.", e);
            throw e;
        }
    }

    @Override
    public void writeStream(String conversationId, String filename, InputStream inputStream, long size) throws IOException {
        String filedir = getConversationFiledirPath(conversationId);
        File localFile = new File(filedir+filename);
        localFile.getParentFile().mkdirs();

        try (FileOutputStream os = new FileOutputStream(localFile);
            BufferedOutputStream bos = new BufferedOutputStream(os)) {
            IOUtils.copy(inputStream,bos);
        } catch (IOException e) {
            log.error("Could not write asic container to disk.", e);
            throw e;
        }
    }

    @Override
    public byte[] read(String conversationId, String filename) throws IOException {
        String filedir = getConversationFiledirPath(conversationId);
        File file = new File(filedir+filename);
        return FileUtils.readFileToByteArray(file);
    }

    @Override
    public FileEntryStream readStream(String conversationId, String filename) throws PersistenceException {
        String filedir = getConversationFiledirPath(conversationId);
        File file = new File(filedir+filename);
        try {
            BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
            return FileEntryStream.of(fis, file.length());
        } catch (FileNotFoundException e) {
            throw new PersistenceException(String.format("File \"%s\" not found for conversationId \"%s\"", filename, conversationId));
        }
    }

    @Override
    public void delete(String conversationId) throws IOException {
        File dir = new File(getConversationFiledirPath(conversationId));
        FileUtils.deleteDirectory(dir);
    }

    private String getConversationFiledirPath(String conversationId) {
        String filedir = props.getNextmove().getFiledir();
        if (!filedir.endsWith("/")) {
            filedir = filedir+"/";
        }
        return filedir+conversationId+"/";
    }
}
