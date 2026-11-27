package no.difi.meldingsutveksling.nextmove.message;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileMessagePersisterTest {

    @Mock
    private IntegrasjonspunktProperties props;

    @Mock
    private IntegrasjonspunktProperties.NextMove nextMove;

    @InjectMocks
    private FileMessagePersister persister;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(props.getNextmove()).thenReturn(nextMove);
        when(nextMove.getFiledir()).thenReturn(tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // --- write ---

    @Test
    void write_storesFileContentAtCorrectPath() throws IOException {
        byte[] content = "hello world".getBytes();

        persister.write("msg-123", "payload.bin", new ByteArrayResource(content));

        assertThat(Files.readAllBytes(tempDir.resolve("msg-123/payload.bin"))).isEqualTo(content);
    }

    @Test
    void write_createsParentDirectories() throws IOException {
        persister.write("msg-abc", "file.txt", new ByteArrayResource("data".getBytes()));

        assertThat(tempDir.resolve("msg-abc").toFile()).isDirectory();
    }

    @Test
    void write_worksWhenFiledirHasTrailingSlash() throws IOException {
        when(nextMove.getFiledir()).thenReturn(tempDir + "/");

        persister.write("msg-789", "data.bin", new ByteArrayResource("hello".getBytes()));

        assertThat(tempDir.resolve("msg-789/data.bin").toFile()).exists();
    }

    @Test
    void write_throwsForNullMessageId() {
        assertThatThrownBy(() -> persister.write(null, "file.txt", new ByteArrayResource(new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    void write_throwsForEmptyMessageId() {
        assertThatThrownBy(() -> persister.write("", "file.txt", new ByteArrayResource(new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "../evil", "a/b", "a\\b"})
    void write_throwsForPathTraversalInMessageId(String messageId) {
        assertThatThrownBy(() -> persister.write(messageId, "file.txt", new ByteArrayResource(new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("illegal characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "../evil", "a/b", "a\\b"})
    void write_throwsForPathTraversalInFilename(String filename) {
        assertThatThrownBy(() -> persister.write("msg-123", filename, new ByteArrayResource(new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("illegal characters");
    }

    // --- read ---

    @Test
    void read_returnsResourcePointingToCorrectPath() throws IOException {
        Resource resource = persister.read("msg-123", "payload.bin");

        assertThat(resource.getFile().toPath()).isEqualTo(tempDir.resolve("msg-123/payload.bin"));
    }

    @Test
    void read_throwsForNullMessageId() {
        assertThatThrownBy(() -> persister.read(null, "file.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "../evil", "a/b", "a\\b"})
    void read_throwsForPathTraversalInMessageId(String messageId) {
        assertThatThrownBy(() -> persister.read(messageId, "file.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("illegal characters");
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "../evil", "a/b", "a\\b"})
    void read_throwsForPathTraversalInFilename(String filename) {
        assertThatThrownBy(() -> persister.read("msg-123", filename))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("illegal characters");
    }

    // --- delete ---

    @Test
    void delete_removesMessageDirectory() throws IOException {
        Path messageDir = tempDir.resolve("msg-123");
        Files.createDirectory(messageDir);
        Files.write(messageDir.resolve("file.txt"), "content".getBytes());

        persister.delete("msg-123");

        assertThat(messageDir.toFile()).doesNotExist();
    }

    @Test
    void delete_doesNotThrowWhenDirectoryDoesNotExist() throws IOException {
        persister.delete("nonexistent-msg");
    }

    @Test
    void delete_throwsForNullMessageId() {
        assertThatThrownBy(() -> persister.delete(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "../evil", "a/b", "a\\b"})
    void delete_throwsForPathTraversalInMessageId(String messageId) {
        assertThatThrownBy(() -> persister.delete(messageId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("illegal characters");
    }

    @Test
    void delete_inTransaction_removesMessageDirectoryOnlyAfterCommit() throws IOException {
        Path messageDir = createMessageDir("msg-123");
        TransactionSynchronizationManager.initSynchronization();

        persister.delete("msg-123");
        assertThat(messageDir.toFile()).exists();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(messageDir.toFile()).doesNotExist();
    }

    @Test
    void delete_inTransaction_keepsMessageDirectoryOnRollback() throws IOException {
        Path messageDir = createMessageDir("msg-123");
        TransactionSynchronizationManager.initSynchronization();

        persister.delete("msg-123");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(messageDir.toFile()).exists();
        assertThat(messageDir.resolve("file.txt")).hasContent("content");
    }

    @Test
    void delete_inTransaction_validatesMessageIdImmediately() {
        TransactionSynchronizationManager.initSynchronization();

        assertThatThrownBy(() -> persister.delete("../evil"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    void delete_inTransaction_failureAfterCommitIsNotPropagated() throws IOException {
        // A plain file where the message directory should be makes FileUtils.deleteDirectory throw.
        Files.write(tempDir.resolve("msg-123"), "not a directory".getBytes());
        TransactionSynchronizationManager.initSynchronization();

        persister.delete("msg-123");

        assertThatCode(() -> TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit))
                .doesNotThrowAnyException();
    }

    private Path createMessageDir(String messageId) throws IOException {
        Path messageDir = tempDir.resolve(messageId);
        Files.createDirectory(messageDir);
        Files.write(messageDir.resolve("file.txt"), "content".getBytes());
        return messageDir;
    }

}
