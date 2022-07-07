package no.difi.meldingsutveksling.nextmove.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.move.common.io.ResourceUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.nextmove.useDbPersistence", havingValue = "false")
public class FileMessagePersister implements MessagePersister {

    private final IntegrasjonspunktProperties props;

    @Override
    public void write(String messageId, String filename, Resource resource) throws IOException {
        String filedir = getMessageFiledirPath(messageId);
        File localFile = new File(filedir + filename);
        localFile.getParentFile().mkdirs();

        try (FileOutputStream os = new FileOutputStream(localFile);
             BufferedOutputStream bos = new BufferedOutputStream(os)) {
            int bytes = ResourceUtils.copy(resource, bos);
            bos.flush();
            log.debug("Storing {} for message[id={}]: {}", filename, messageId, humanReadableByteCount(bytes));
        } catch (IOException e) {
            log.error("Could not write asic container to disk.", e);
            throw e;
        }
    }

    @Override
    public Resource read(String messageId, String filename) throws IOException {
        String filedir = getMessageFiledirPath(messageId);
        File file = new File(filedir + filename);
        return new FileSystemResource(file);
    }

    @Override
    public void delete(String messageId) throws IOException {
        File dir = new File(getMessageFiledirPath(messageId));
        log.debug("Deleting directory {} for message[id={}]", dir.getAbsolutePath(), messageId);
        FileUtils.deleteDirectory(dir);
    }

    private String humanReadableByteCount(int bytes) {
        int unit = 1000;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "kMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    private String getMessageFiledirPath(String messageId) {
        String filedir = props.getNextmove().getFiledir();
        if (!filedir.endsWith("/")) {
            filedir = filedir + "/";
        }
        return filedir + messageId + "/";
    }
}
