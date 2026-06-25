package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.CryptoMessagePersister;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.FileNotFoundException;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInController;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInService;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.time.Clock;
import java.time.OffsetDateTime;

import static no.difi.meldingsutveksling.nextmove.StandardBusinessDocumentTestData.ARKIVMELDING_SBD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * When asic.zip can't be read, popMessage deletes the message and throws AsicPersistenceException, which
 * the controller rethrows as FileNotFoundException and rolls its transaction back. The delete must survive
 * that rollback (it runs with REQUIRES_NEW); otherwise the message reappears on every poll.
 *
 * Not @DataJpaTest and not @Transactional on purpose: the saved message must be committed for the
 * REQUIRES_NEW transaction to see it, and the delete must be observable after the rollback.
 */
@SpringBootTest(classes = NextMovePopMessageRollbackTest.TestApp.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:poptx;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NextMovePopMessageRollbackTest {

    @Autowired
    private NextMoveMessageInController controller;
    @Autowired
    private NextMoveMessageInRepository repo;

    @MockitoBean
    private CryptoMessagePersister cryptoMessagePersister;
    @MockitoBean
    private ConversationService conversationService;

    @AfterEach
    void cleanup() {
        repo.deleteAll();
    }

    @Test
    void corruptAsic_messageIsDeletedAndStaysDeleted_afterControllerRollback() throws Exception {
        StandardBusinessDocument sbd = ARKIVMELDING_SBD;
        String messageId = sbd.getMessageId();

        NextMoveInMessage message = NextMoveInMessage.of(sbd, ServiceIdentifier.DPO);
        message.setLockTimeout(OffsetDateTime.now()); // locked, so popMessage proceeds
        repo.save(message);
        assertThat(repo.findByMessageId(messageId)).isPresent();

        given(cryptoMessagePersister.read(messageId, NextMoveConsts.ASIC_FILE))
                .willThrow(new IOException("asic.zip missing on disk"));

        assertThatThrownBy(() -> controller.popMessage(messageId, new MockHttpServletResponse()))
                .isInstanceOf(FileNotFoundException.class);

        assertThat(repo.findByMessageId(messageId))
                .as("delete should be committed despite the controller rollback")
                .isEmpty();

        verify(conversationService).registerStatus(eq(messageId), eq(ReceiptStatus.FEIL), anyString());
    }

    @Configuration
    @ImportAutoConfiguration({
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            JpaRepositoriesAutoConfiguration.class,
            TransactionAutoConfiguration.class,
            JacksonAutoConfiguration.class
    })
    @EntityScan(basePackageClasses = NextMoveInMessage.class)
    @EnableJpaRepositories(basePackageClasses = NextMoveMessageInRepository.class)
    @Import({NextMoveMessageInController.class, JacksonConfig.class})
    static class TestApp {

        @Bean
        Clock clock() {
            return Clock.systemUTC(); // required by JacksonConfig
        }

        // Hand-built so IntegrasjonspunktProperties stays out of the context: as a @Validated
        // @ConfigurationProperties bean it would be bound against difi.move and fail. popMessage ignores it.
        @Bean
        NextMoveMessageInService nextMoveMessageInService(ConversationService conversationService,
                                                          NextMoveMessageInRepository messageRepo,
                                                          CryptoMessagePersister cryptoMessagePersister,
                                                          Clock clock) {
            return new NextMoveMessageInService(new IntegrasjonspunktProperties(), conversationService,
                    messageRepo, cryptoMessagePersister,
                    org.mockito.Mockito.mock(ResponseStatusSender.class), clock);
        }
    }
}
