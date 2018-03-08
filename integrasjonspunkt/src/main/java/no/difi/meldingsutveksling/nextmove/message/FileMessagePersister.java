package no.difi.meldingsutveksling.nextmove.message;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Component
@Slf4j
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

        try (FileOutputStream os = new FileOutputStream(localFile);
             BufferedOutputStream bos = new BufferedOutputStream(os)) {
            bos.write(message);
        } catch (IOException e) {
            log.error("Could not write asic container to disc.", e);
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
