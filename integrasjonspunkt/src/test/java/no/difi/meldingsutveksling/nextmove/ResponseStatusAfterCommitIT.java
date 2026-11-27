package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.CryptoMessagePersister;
import no.difi.meldingsutveksling.clock.ClockConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInService;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumSet;

import static no.difi.meldingsutveksling.nextmove.StandardBusinessDocumentTestData.ARKIVMELDING_SBD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * deleteMessage sends status LEVERT to the sender. If the surrounding transaction (e.g. the controller's)
 * rolls back, the message is still on the queue, so the sender must not be told LEVERT (MOVE-5500).
 * <p>
 * Not @DataJpaTest and not @Transactional on purpose: the test controls commit/rollback itself.
 */
@SpringBootTest(classes = {
    ResponseStatusAfterCommitIT.TestApp.class,
    ClockConfig.class,
}, properties = {
    "spring.datasource.url=jdbc:h2:mem:statusaftercommit;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ResponseStatusAfterCommitIT {

    @Autowired
    private NextMoveMessageInService service;
    @Autowired
    private NextMoveMessageInRepository repo;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private ResponseStatusSenderProxy proxy;
    @MockitoBean
    private CryptoMessagePersister cryptoMessagePersister;
    @MockitoBean
    private ConversationService conversationService;

    private String messageId;

    @BeforeEach
    void saveLockedMessage() {
        StandardBusinessDocument sbd = ARKIVMELDING_SBD;
        messageId = sbd.getMessageId();
        NextMoveInMessage message = NextMoveInMessage.of(sbd, ServiceIdentifier.DPO);
        message.setLockTimeout(OffsetDateTime.now()); // locked, so deleteMessage proceeds
        repo.save(message);
    }

    @AfterEach
    void cleanup() {
        repo.deleteAll();
    }

    @Test
    void deleteMessage_rolledBack_noStatusIsSent() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.deleteMessage(messageId);
            status.setRollbackOnly();
        });

        assertThat(repo.findByMessageId(messageId))
            .as("delete was rolled back, message is still on the queue")
            .isPresent();
        verify(proxy, after(500).never()).queue(any(), any(), any());
    }

    @Test
    void deleteMessage_committed_levertIsSent() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> service.deleteMessage(messageId));

        assertThat(repo.findByMessageId(messageId)).isEmpty();
        verify(proxy, timeout(5000)).queue(any(), eq(ServiceIdentifier.DPO), eq(ReceiptStatus.LEVERT));
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
    @Import(JacksonConfig.class)
    static class TestApp {

        // Hand-built so IntegrasjonspunktProperties stays out of the context: as a @Validated
        // @ConfigurationProperties bean it would be bound against difi.move and fail.
        private static IntegrasjonspunktProperties props() {
            IntegrasjonspunktProperties props = new IntegrasjonspunktProperties();
            props.setNextmove(new IntegrasjonspunktProperties.NextMove());
            props.getNextmove().setStatusServices(EnumSet.of(ServiceIdentifier.DPO));
            return props;
        }

        @Bean
        ResponseStatusSender responseStatusSender(ResponseStatusSenderProxy proxy) {
            return new ResponseStatusSender(proxy, props());
        }

        @Bean
        NextMoveMessageInService nextMoveMessageInService(ConversationService conversationService,
                                                          NextMoveMessageInRepository messageRepo,
                                                          CryptoMessagePersister cryptoMessagePersister,
                                                          ResponseStatusSender responseStatusSender,
                                                          Clock clock) {
            return new NextMoveMessageInService(props(), conversationService, messageRepo,
                cryptoMessagePersister, responseStatusSender, clock);
        }
    }
}
