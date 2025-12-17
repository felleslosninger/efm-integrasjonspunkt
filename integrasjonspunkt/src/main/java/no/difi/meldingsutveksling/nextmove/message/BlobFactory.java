package no.difi.meldingsutveksling.nextmove.message;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.TmpFile;
import org.hibernate.LobHelper;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import jakarta.persistence.EntityManager;

import javax.sql.rowset.serial.SerialBlob;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlobFactory {

    private boolean useTemporaryFile = false;

    Blob createBlob(InputStream inputStream, Long size) throws IOException {
        if (size == -1) {
            if (useTemporaryFile) {
                return createBlobUsingTemporaryFile(inputStream);
            }
            return createBlob(inputStream);
        }

        return createBlobWithKnownSize(inputStream, size);
    }

    @SneakyThrows
    private Blob createBlobWithKnownSize(InputStream inputStream, Long size) {
        return new SerialBlob(inputStream.readAllBytes());
    }

    private Blob createBlob(InputStream inputStream) throws IOException {
        try {
            return new SerialBlob(inputStream.readAllBytes());
        } catch (SQLException | UnsupportedOperationException e) {
            log.info("Could not create BLOB of unknown size. Switching to temporary file before BLOB creation. Exception was: {}", e.getLocalizedMessage());
            useTemporaryFile = true;
            return createBlobUsingTemporaryFile(inputStream);
        }
    }

    private Blob createBlobUsingTemporaryFile(InputStream inputStream) throws IOException {
        TmpFile tmpFile = TmpFile.create(inputStream);
        File file = tmpFile.getFile();
        AutoClosingInputStream is = new AutoClosingInputStream(tmpFile.getInputStream(), tmpFile::delete);
        return createBlob(is, file.length());
    }
}
