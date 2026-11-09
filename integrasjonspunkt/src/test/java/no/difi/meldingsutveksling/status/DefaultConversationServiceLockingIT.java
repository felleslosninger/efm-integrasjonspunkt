package no.difi.meldingsutveksling.status;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.mail.IpMailSender;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.receipt.StatusQueue;
import no.difi.meldingsutveksling.webhooks.WebhookPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static no.difi.meldingsutveksling.receipt.ReceiptStatus.LEVERT;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.MOTTATT;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.SENDT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DPI status polling (DpiStatusStrategy) and DPI receipt polling (DpiReceiptHandler) can both register MOTTATT for the
 * same message. Without locking and re-reading, the duplicate check in registerStatus runs against a stale copy of the
 * conversation and MOTTATT is registered twice.
 */
@ResourceLock("nextmove-plain-datajpatest")
@DataJpaTest
@Import(DefaultConversationService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DefaultConversationServiceLockingIT {

    private static final String CONVERSATION_ID = "conv-dpi-lock";
    private static final String MESSAGE_ID = "msg-dpi-lock";

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private DefaultConversationService conversationService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private IntegrasjonspunktProperties props;
    @MockitoBean
    private WebhookPublisher webhookPublisher;
    @MockitoBean
    private MessageStatusFactory messageStatusFactory;
    @MockitoBean
    private IpMailSender ipMailSender;
    @MockitoBean
    private Clock clock;
    @MockitoBean
    private StatusQueue statusQueue;

    @AfterEach
    void tearDown() {
        conversationRepository.deleteAll();
    }

    @Test
    void registerStatusesWithLockIgnoresStatusRegisteredAfterConversationWasLoaded() {
        Long id = saveSentDpiConversation();
        // Loaded at the start of a status polling run, detached from here on
        Conversation stale = conversationRepository.findById(id).orElseThrow();

        // Meanwhile DpiReceiptHandler receives LEVERT and registers MOTTATT before it
        conversationService.registerStatus(MESSAGE_ID, MessageStatus.of(MOTTATT, OffsetDateTime.now().minusSeconds(1)));
        conversationService.registerStatus(MESSAGE_ID, MessageStatus.of(LEVERT, OffsetDateTime.now()));

        // Status polling then gets SENDT (-> MOTTATT) from corner 2
        conversationService.registerStatusesWithLock(stale,
            List.of(MessageStatus.of(MOTTATT, OffsetDateTime.now(), "Hjørne 3 har mottatt meldingen")));

        assertThat(statusesOf(id)).containsExactlyInAnyOrder("SENDT", "MOTTATT", "LEVERT");
    }

    @Test
    void registerStatusesWithLockWaitsForConcurrentTransactionHoldingTheLock() throws Exception {
        Long id = saveSentDpiConversation();
        Conversation stale = conversationRepository.findById(id).orElseThrow();

        CountDownLatch locked = new CountDownLatch(1);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CompletableFuture<Void> receiptHandler = CompletableFuture.runAsync(() -> tx.executeWithoutResult(status -> {
            Conversation c = conversationService.findConversationForUpdate(CONVERSATION_ID, ConversationDirection.OUTGOING).orElseThrow();
            locked.countDown();
            sleep(300);
            conversationService.registerStatus(c, MessageStatus.of(MOTTATT, OffsetDateTime.now()));
        }));

        assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
        conversationService.registerStatusesWithLock(stale, List.of(MessageStatus.of(MOTTATT, OffsetDateTime.now())));
        receiptHandler.get(10, TimeUnit.SECONDS);

        assertThat(statusesOf(id)).containsExactlyInAnyOrder("SENDT", "MOTTATT");
    }

    @Test
    void findConversationForUpdateRequiresTransaction() {
        saveSentDpiConversation();

        assertThatThrownBy(() -> conversationService.findConversationForUpdate(CONVERSATION_ID, ConversationDirection.OUTGOING))
            .isInstanceOf(IllegalTransactionStateException.class);
    }

    private Long saveSentDpiConversation() {
        Conversation conversation = new Conversation();
        conversation.setConversationId(CONVERSATION_ID);
        conversation.setMessageId(MESSAGE_ID);
        conversation.setDirection(ConversationDirection.OUTGOING);
        conversation.setServiceIdentifier(ServiceIdentifier.DPI);
        conversation.setPollable(true);
        conversation.setMessageStatuses(new HashSet<>());
        Long id = conversationRepository.save(conversation).getId();
        conversationService.registerStatus(MESSAGE_ID, MessageStatus.of(SENDT, OffsetDateTime.now()));
        return id;
    }

    private List<String> statusesOf(Long id) {
        return conversationRepository.findById(id).orElseThrow()
            .getMessageStatuses().stream()
            .map(MessageStatus::getStatus)
            .toList();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
