package no.difi.meldingsutveksling.nextmove.message;

import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.NextMoveMessageEntry;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.nextmove.use-db-persistence", havingValue = "true")
@RequiredArgsConstructor
public class DBMessagePersister implements MessagePersister {

    private static final String CACHE_DIR = "integrasjonspunkt/db-blob-cache";

    private final BlobFactory blobFactory;
    private final NextMoveMessageEntryRepository repo;
    private final IntegrasjonspunktProperties properties;

    @Override
    @Transactional
    public void write(String messageId, String filename, Resource resource) throws IOException {
        long size = contentLengthOrUnknown(resource);
        try (InputStream inputStream = resource.getInputStream()) {
            Blob blob = blobFactory.createBlob(inputStream, size);
            NextMoveMessageEntry entry = NextMoveMessageEntry.of(messageId, filename, blob, size);
            // saveAndFlush, not save: the Blob only wraps the stream - the driver reads it when the
            // INSERT executes. A plain save defers the INSERT to transaction commit, by which time this
            // try-with-resources has closed the stream and the driver fails with ClosedChannelException
            // (killing the connection mid-protocol). Flushing executes the INSERT while the stream is open.
            repo.saveAndFlush(entry);
        }
    }

    @Override
    @Transactional(readOnly = true) // readOnly will not actually be applied when joining existsing transaction
    public Resource read(String messageId, String filename) throws IOException {
        // NOTE : When throwing PersistenceException (which is a RuntimeException) the active transaction
        // will be marked for rollback even if we later catch it explicitly.
        NextMoveMessageEntry entry = repo.findByMessageIdAndFilename(messageId, filename).findFirst()
                .orElseThrow(() -> new PersistenceException("Entry for messageId=%s, filename=%s not found in database".formatted(messageId, filename)));

        // Stream the LOB to a local temp file while the transaction (and its JDBC connection) is still open,
        // then hand back a connection-independent FileSystemResource.
        //
        // Why a temp file and not the LOB directly:
        //  - The payload is streamed (small buffer), never held whole in heap -> avoids OutOfMemoryError.
        //  - The bytes are detached from the JDBC connection, so the resource can be consumed lazily AFTER this
        //    transaction commits and the connection is returned to the pool. A connection-bound LOB locator can
        //    not (e.g. PostgreSQL large objects fail with "cannot read LOB after the transaction"). Several
        //    consumers - notably the outgoing send pipeline - read the resource outside any transaction.
        //  - FileSystemResource is freely re-readable, so probes such as exists()/isReadable()/contentLength()
        //    followed by the real read all work.
        //
        // The temp file lives until the message is deleted (see delete()).
        //
        // The copy goes to a unique temp name and is atomically renamed into place. Writing directly to the
        // target path would truncate-in-place: a concurrent read() of the same file (pop retry, lock expiry
        // mid-transfer, duplicate send) would corrupt the stream of a reader that already has the file open.
        // With rename, concurrent writers each produce their own complete (identical) copy, and an in-flight
        // reader keeps its old inode untouched until it closes.
        File target = cacheFileFor(messageId, filename);
        File tmp = new File(target.getParentFile(), target.getName() + "." + UUID.randomUUID() + ".tmp");
        copyBlobToFile(entry.getContent(), tmp);
        moveIntoPlace(tmp, target);
        return new FileSystemResource(target);
    }

    @Override
    @Transactional
    public void delete(String messageId) throws IOException {
        repo.deleteByMessageId(messageId);
        deleteCache(messageId);
    }

    private void copyBlobToFile(Blob blob, File target) throws IOException {
        try (InputStream blobStream = blob.getBinaryStream()) {
            FileUtils.copyInputStreamToFile(blobStream, target);
        } catch (SQLException e) {
            FileUtils.deleteQuietly(target);
            throw new IOException("Could not read BLOB from database", e);
        } catch (IOException e) {
            // Do not leave a half-written cache file behind on failure.
            FileUtils.deleteQuietly(target);
            throw e;
        }
    }

    /**
     * Atomically renames the freshly copied temp file onto the target path. On POSIX the rename swaps the
     * directory entry only, so a reader that already has the old file open keeps reading its (complete) copy.
     * Windows may refuse to replace a target that another reader has open - in that case the existing target
     * is a complete copy of the same blob, so the temp file is simply discarded and the target served as-is.
     */
    private static void moveIntoPlace(File tmp, File target) throws IOException {
        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            FileUtils.deleteQuietly(tmp);
            if (!target.exists()) {
                throw e;
            }
        }
    }

    private void deleteCache(String messageId) {
        File dir = cacheDirectoryFor(messageId);
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            log.warn("Could not delete temp blob cache {} for messageId={}", dir, messageId, e);
        }
    }

    /**
     * Base directory for the blob cache. Uses the configured {@code difi.move.nextmove.blob-cache-dir}
     * when set, otherwise the system temp directory. In containers this should be a sized, disk-backed
     * (not tmpfs) volume.
     */
    private File cacheBaseDir() {
        String configured = properties.getNextmove().getBlobCacheDir();
        String base = StringUtils.hasText(configured) ? configured : FileUtils.getTempDirectoryPath();
        return new File(base, CACHE_DIR);
    }

    /**
     * The directory that holds all cached blob files for a single message. The directory name is the
     * SHA-256 hash of the messageId, so no user-controlled characters ever reach the file system path
     * (the hash is fixed-length lowercase hex); the containment check guarantees the path stays inside
     * the cache directory.
     */
    private File cacheDirectoryFor(String messageId) {
        Path base = cacheBaseDir().toPath().normalize().toAbsolutePath();
        // The hash is always 64 lowercase hex characters regardless of input, so no user-controlled
        // characters (path separators, '..') can ever reach the path.
        Path dir = base.resolve(DigestUtils.sha256Hex(messageId)).normalize().toAbsolutePath();
        if (!dir.startsWith(base)) {
            throw new IllegalArgumentException("Blob cache path escapes the cache directory");
        }
        return dir.toFile();
    }

    /** A single cached blob file (one filename, also hashed) inside the message's cache directory. */
    private File cacheFileFor(String messageId, String filename) {
        return new File(cacheDirectoryFor(messageId), DigestUtils.sha256Hex(filename));
    }

    /**
     * Returns the resource length when it can be obtained cheaply and safely, or {@code -1} when it is unknown.
     *
     * <p>Resources reporting {@code isOpen() == true} (InputStreamResource, pipe-backed resources such as the
     * CMS-encrypted output) wrap a single-use stream that may only be opened once - by {@link #write} - so their
     * length is never probed. For other types the length is only probed when the type is known to report it
     * cheaply without touching the stream: Spring's default {@code AbstractResource.contentLength()} READS THE
     * WHOLE STREAM to count bytes, which would consume single-use resources and waste IO on the rest. Unknown
     * types are therefore treated as unknown length rather than probed.
     */
    private static long contentLengthOrUnknown(Resource resource) {
        if (resource.isOpen()) {
            return -1L;
        }
        if (resource instanceof FileSystemResource || resource instanceof ByteArrayResource) {
            try {
                long length = resource.contentLength();
                return length >= 0 ? length : -1L;
            } catch (IOException e) {
                return -1L;
            }
        }
        return -1L;
    }
}
