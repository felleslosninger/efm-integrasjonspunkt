package no.difi.meldingsutveksling;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

@Data
@Slf4j
public class TmpFile {

    private String id;

    private TmpFile() {
    }

    public static TmpFile create() {
        TmpFile tmpFile = new TmpFile();
        tmpFile.setId(UUID.randomUUID().toString());

        if (!(new File(getTmpDirPath()).mkdirs())) {
            log.debug("Path {} not created", getTmpDirPath());
        }
        log.debug("Created tmp file \"{}\" in folder \"{}\"", tmpFile.getId(), getTmpDirPath());

        return tmpFile;
    }

    public static TmpFile create(InputStream is) throws IOException {
        TmpFile tmpFile = TmpFile.create();
        FileUtils.copyInputStreamToFile(is, tmpFile.getFile());
        return tmpFile;
    }

    public OutputStream getOutputStream() throws IOException {
        return FileUtils.openOutputStream(getFile());
    }

    public InputStream getInputStream() throws IOException {
        return FileUtils.openInputStream(getFile());
    }

    public void delete() {
        try {
            FileUtils.forceDelete(getFile());
        } catch (IOException e) {
            log.warn(String.format("Error deleting files in TmpFile.delete() tmp file %s - make sure streams are closed", getFullPath()), e);
        }
    }

    public File getFile() {
        return new File(getFullPath());
    }

    private String getFullPath() {
        return FilenameUtils.concat(getTmpDirPath(), getId());
    }

    private static String getTmpDirPath() {
        String tmpDir = FilenameUtils.concat(FileUtils.getTempDirectoryPath(), "integrasjonspunkt/");
        log.debug("Current tmp directory: {}", tmpDir);
        return tmpDir;
    }
}
