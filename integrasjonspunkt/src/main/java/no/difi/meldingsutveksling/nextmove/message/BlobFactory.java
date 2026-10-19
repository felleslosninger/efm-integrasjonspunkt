package no.difi.meldingsutveksling.nextmove.message;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.apache.commons.io.FileUtils;
import org.hibernate.LobHelper;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BlobFactory {

    private static final String SPILL_DIR = "integrasjonspunkt/db-blob-spill";

    private final EntityManager em;
    private final IntegrasjonspunktProperties properties;

    /**
     * Creates a database BLOB by streaming the given input, without buffering the whole payload in memory.
     *
     * <p>When the size is known the stream is bound directly to the LOB (Hibernate binds it via
     * {@code PreparedStatement.setBinaryStream(index, stream, length)}). When the size is unknown
     * ({@code size == null} or negative) the stream is first spilled to a temporary file to determine its
     * length and is then streamed into the LOB. Either way heap usage stays bounded.
     */
    Blob createBlob(InputStream inputStream, Long size) throws IOException {
        if (size == null || size < 0) {
            return createBlobUsingTemporaryFile(inputStream);
        }
        return getLobHelper().createBlob(inputStream, size);
    }

    /**
     * Spills the stream to a temporary file to determine its length, then streams the file into the LOB.
     * The file is deleted once it has been fully read (when the LOB is bound at flush time). This is the
     * normal path for outgoing messages, since the length of the CMS-encrypted payload cannot be known
     * until the bytes have been produced.
     */
    private Blob createBlobUsingTemporaryFile(InputStream inputStream) throws IOException {
        File spillFile = new File(spillDir(), UUID.randomUUID().toString());
        try {
            FileUtils.copyInputStreamToFile(inputStream, spillFile);
        } catch (IOException e) {
            FileUtils.deleteQuietly(spillFile);
            throw e;
        }
        AutoClosingInputStream is = new AutoClosingInputStream(
                FileUtils.openInputStream(spillFile),
                () -> FileUtils.deleteQuietly(spillFile));
        return createBlob(is, spillFile.length());
    }

    /**
     * Spill directory for write-side temporary files. Uses the same configurable base directory
     * ({@code difi.move.nextmove.blob-cache-dir}) as the read-side blob cache, so container deployments
     * only need a single writable, disk-backed volume. Defaults to the system temp directory.
     */
    private File spillDir() {
        String configured = properties.getNextmove().getBlobCacheDir();
        String base = StringUtils.hasText(configured) ? configured : FileUtils.getTempDirectoryPath();
        return new File(base, SPILL_DIR);
    }

    private LobHelper getLobHelper() {
        return em.unwrap(Session.class).getLobHelper();
    }
}
