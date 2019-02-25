package no.difi.meldingsutveksling.nextmove.message;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
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
    public void write(ConversationResource cr, String filename, byte[] message) throws IOException {
        String filedir = getConversationFiledirPath(cr);
        File localFile = new File(filedir+filename);
        localFile.getParentFile().mkdirs();

        if (props.getNextmove().getApplyZipHeaderPatch() && ASIC_FILE.equals(filename)){
            BugFix610.applyPatch(message, cr.getConversationId());
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
    public void writeStream(ConversationResource cr, String filename, InputStream inputStream, long size) throws IOException {
        String filedir = getConversationFiledirPath(cr);
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
    public byte[] read(ConversationResource cr, String filename) throws IOException {
        String filedir = getConversationFiledirPath(cr);
        File file = new File(filedir+filename);
        return FileUtils.readFileToByteArray(file);
    }

    @Override
    public FileEntryStream readStream(ConversationResource cr, String filename) throws PersistenceException {
        String filedir = getConversationFiledirPath(cr);
        File file = new File(filedir+filename);
        try {
            BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file));
            return FileEntryStream.of(fis, file.length());
        } catch (FileNotFoundException e) {
            throw new PersistenceException(String.format("File \"%s\" not found for conversationId \"%s\"", filename, cr.getConversationId()));
        }
    }

    @Override
    public void delete(ConversationResource cr) throws IOException {
        File dir = new File(getConversationFiledirPath(cr));
        FileUtils.deleteDirectory(dir);
    }

    private String getConversationFiledirPath(ConversationResource cr) {
        String filedir = props.getNextmove().getFiledir();
        if (!filedir.endsWith("/")) {
            filedir = filedir+"/";
        }
        return filedir+cr.getConversationId()+"/";
    }
}
