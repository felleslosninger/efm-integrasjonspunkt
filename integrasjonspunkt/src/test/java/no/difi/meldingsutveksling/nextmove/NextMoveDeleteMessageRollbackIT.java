package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.CryptoMessagePersister;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.clock.ClockConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.message.FileMessagePersister;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInController;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;

import static no.difi.meldingsutveksling.nextmove.StandardBusinessDocumentTestData.ARKIVMELDING_SBD;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * deleteMessage deletes the message row and its files. Deleting files is not transactional, so if the
 * surrounding transaction is rolled back the files must survive with the row; otherwise the next pop finds a
 * message without asic.zip and marks it FEIL.
 * <p>
 * Not @DataJpaTest and not @Transactional on purpose: the transaction boundaries are controlled by the test.
 */
@SpringBootTest(classes = {
    NextMoveDeleteMessageRollbackIT.TestApp.class,
    ClockConfig.class,
}, properties = {
    "spring.datasource.url=jdbc:h2:mem:deletetx;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NextMoveDeleteMessageRollbackIT {

    // Created before the Spring context, which needs it for the FileMessagePersister bean.
    private static final Path filedir = createTempDir();

    @Autowired
    private NextMoveMessageInController controller;
    @Autowired
    private NextMoveMessageInRepository repo;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private MessagePersister messagePersister;

    @MockitoBean
    private ConversationService conversationService;

    private String messageId;

    @BeforeEach
    void setUp() throws IOException {
        StandardBusinessDocument sbd = ARKIVMELDING_SBD;
        messageId = sbd.getMessageId();

        NextMoveInMessage message = NextMoveInMessage.of(sbd, ServiceIdentifier.DPO);
        message.setLockTimeout(OffsetDateTime.now()); // locked, so deleteMessage proceeds
        repo.save(message);
        messagePersister.write(messageId, NextMoveConsts.ASIC_FILE, new ByteArrayResource("asic".getBytes()));
        assertThat(asicFile()).exists();
    }

    @AfterEach
    void cleanup() {
        repo.deleteAll();
    }

    @AfterAll
    static void deleteFiledir() {
        FileUtils.deleteQuietly(filedir.toFile());
    }

    private static Path createTempDir() {
        try {
            return Files.createTempDirectory("delete-rollback-it");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void rollback_keepsMessageAndFiles() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            controller.deleteMessage(messageId);
            status.setRollbackOnly();
        });

        assertThat(repo.findByMessageId(messageId)).isPresent();
        assertThat(asicFile())
            .as("files must survive the rollback together with the message row")
            .exists();
    }

    @Test
    void commit_deletesMessageAndFiles() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
            controller.deleteMessage(messageId));

        assertThat(repo.findByMessageId(messageId)).isEmpty();
        assertThat(asicFile().getParent()).doesNotExist();
    }

    private Path asicFile() {
        return filedir.resolve(messageId).resolve(NextMoveConsts.ASIC_FILE);
    }

    @Configuration
    @ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        DataJpaRepositoriesAutoConfiguration.class,
        TransactionAutoConfiguration.class,
        JacksonAutoConfiguration.class
    })
    @EntityScan(basePackageClasses = NextMoveInMessage.class)
    @EnableJpaRepositories(basePackageClasses = NextMoveMessageInRepository.class)
    @Import({NextMoveMessageInController.class, JacksonConfig.class})
    static class TestApp {

        // Hand-built so IntegrasjonspunktProperties stays out of the context: as a @Validated
        // @ConfigurationProperties bean it would be bound against difi.move and fail.
        private static IntegrasjonspunktProperties props() {
            IntegrasjonspunktProperties props = new IntegrasjonspunktProperties();
            IntegrasjonspunktProperties.NextMove nextMove = new IntegrasjonspunktProperties.NextMove();
            nextMove.setFiledir(filedir.toString());
            props.setNextmove(nextMove);
            return props;
        }

        @Bean
        MessagePersister messagePersister() {
            return new FileMessagePersister(props());
        }

        @Bean
        NextMoveMessageInService nextMoveMessageInService(ConversationService conversationService,
                                                          NextMoveMessageInRepository messageRepo,
                                                          MessagePersister messagePersister,
                                                          Clock clock) {
            CryptoMessagePersister cryptoMessagePersister = new CryptoMessagePersister() {
                @Override
                public void write(String messageId, String filename, Resource resource) throws IOException {
                    messagePersister.write(messageId, filename, resource);
                }

                @Override
                public Resource read(String messageId, String filename) throws IOException {
                    return messagePersister.read(messageId, filename);
                }

                @Override
                public void delete(String messageId) throws IOException {
                    messagePersister.delete(messageId);
                }
            };
            return new NextMoveMessageInService(props(), conversationService, messageRepo, cryptoMessagePersister,
                org.mockito.Mockito.mock(ResponseStatusSender.class), clock);
        }
    }
}
