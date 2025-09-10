package no.difi.meldingsutveksling.nextmove.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.TmpFile;
import org.hibernate.LobHelper;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import jakarta.persistence.EntityManager;
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

    private final EntityManager em;
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

    private Blob createBlobWithKnownSize(InputStream inputStream, Long size) {
        return getLobHelper().createBlob(inputStream, size);
    }

    private Blob createBlob(InputStream inputStream) throws IOException {

        Blob blob = getLobHelper().createBlob(new byte[0]);

        try (OutputStream outputStream = blob.setBinaryStream(1)) {
            FileCopyUtils.copy(inputStream, outputStream);
        } catch (SQLException | UnsupportedOperationException e) {
            log.info("Could not create BLOB of unknown size. Switching to temporary file before BLOB creation. Exception was: {}", e.getLocalizedMessage());
            useTemporaryFile = true;
            return createBlobUsingTemporaryFile(inputStream);
        }

        return blob;
    }

    private Blob createBlobUsingTemporaryFile(InputStream inputStream) throws IOException {
        TmpFile tmpFile = TmpFile.create(inputStream);
        File file = tmpFile.getFile();
        AutoClosingInputStream is = new AutoClosingInputStream(tmpFile.getInputStream(), tmpFile::delete);
        return createBlob(is, file.length());
    }

    public Blob createBlob(byte[] message) {
        LobHelper lobHelper = getLobHelper();
        return lobHelper.createBlob(message);
    }

    private LobHelper getLobHelper() {
        return em.unwrap(Session.class).getLobHelper();
    }
}
