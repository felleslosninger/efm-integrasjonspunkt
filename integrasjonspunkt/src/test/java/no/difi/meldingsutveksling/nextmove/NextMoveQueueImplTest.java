package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.altinnv3.dpo.MessageChannelRepository;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.api.NextMoveQueue.EnqueueResult;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NextMoveQueueImplTest {

    private static final String MESSAGE_ID = "c4b0f4a6-0a3b-4d3c-9f0e-2a1b7c9d8e01";

    @Mock
    private NextMoveMessageInRepository messageRepo;
    @Mock
    private ConversationService conversationService;
    @Mock
    private SBDService sbdService;
    @Mock
    private MessagePersister messagePersister;
    @Mock
    private TimeToLiveHelper timeToLiveHelper;
    @Mock
    private ResponseStatusSender statusSender;
    @Mock
    private MessageChannelRepository messageChannelRepository;

    private NextMoveQueueImpl target;
    private StandardBusinessDocument sbd;

    @BeforeEach
    void setUp() {
        target = new NextMoveQueueImpl(messageRepo, conversationService, sbdService, messagePersister,
            timeToLiveHelper, statusSender, messageChannelRepository);
        sbd = mock(StandardBusinessDocument.class);
        lenient().when(sbd.getMessageId()).thenReturn(MESSAGE_ID);
        lenient().when(sbd.getType()).thenReturn("arkivmelding");
        lenient().when(sbd.getAny()).thenReturn(new ArkivmeldingMessage());
    }

    @Test
    void duplicate_is_discarded_and_its_asic_is_read_to_the_end_without_being_written() throws Exception {
        when(messageRepo.findByMessageId(MESSAGE_ID)).thenReturn(Optional.of(mock(NextMoveInMessage.class)));
        ByteArrayInputStream asic = new ByteArrayInputStream(new byte[100_000]);

        EnqueueResult result = target.enqueueIncomingMessage(sbd, ServiceIdentifier.DPF, new InputStreamResource(asic));

        assertThat(result).isEqualTo(EnqueueResult.DUPLICATE);
        assertThat(asic.available()).isZero();
        verify(messagePersister, never()).write(anyString(), anyString(), any(Resource.class));
        verify(messageRepo, never()).save(any());
    }

    @Test
    void expired_message_is_not_enqueued_and_its_asic_is_read_to_the_end() {
        when(sbdService.isExpired(sbd)).thenReturn(true);
        ByteArrayInputStream asic = new ByteArrayInputStream(new byte[100_000]);

        EnqueueResult result = target.enqueueIncomingMessage(sbd, ServiceIdentifier.DPF, new InputStreamResource(asic));

        assertThat(result).isEqualTo(EnqueueResult.EXPIRED);
        assertThat(asic.available()).isZero();
        verify(messageRepo, never()).save(any());
    }

    @Test
    void status_is_registered_on_its_conversation_and_not_enqueued() {
        when(sbd.getType()).thenReturn("status");
        when(sbd.getAny()).thenReturn(new StatusMessage(ReceiptStatus.LEVERT));

        EnqueueResult result = target.enqueueIncomingMessage(sbd, ServiceIdentifier.DPO);

        assertThat(result).isEqualTo(EnqueueResult.STATUS);
        verify(conversationService).registerStatus(MESSAGE_ID, ReceiptStatus.LEVERT);
        verify(messageRepo, never()).save(any());
    }
}
