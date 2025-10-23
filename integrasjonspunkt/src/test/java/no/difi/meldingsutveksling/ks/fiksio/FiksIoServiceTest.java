package no.difi.meldingsutveksling.ks.fiksio;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.FiksIoMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.model.MeldingId;
import no.ks.fiks.io.client.model.MeldingRequest;
import no.ks.fiks.io.client.model.SendtMelding;
import no.ks.fiks.io.client.model.StringPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class FiksIoServiceTest {

    @Mock
    private FiksIOKlient fiksIOKlient;
    @Mock
    private ServiceRegistryLookup serviceRegistryLookup;
    @Mock
    private OptionalCryptoMessagePersister persister;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private ConversationService conversationService;

    private FiksIoService fiksIoService;

    @BeforeEach
    void before() {
        fiksIoService = new FiksIoService(fiksIOKlient, serviceRegistryLookup, persister, conversationService);
    }

    @Test
    void send_message_ok() throws Exception {
        Iso6523 iso6523 = Iso6523.parse("0192:910076787");
        String kontoId = "d49177d3-ec0c-40ee-ace9-0f2781a05f45";
        String messageId = "0e238873-63ba-4993-84e1-73b91eb2061d";
        String convId = "c9f37b22-cf8a-44de-b854-050f6a9acc7a";
        String protocol = "digdir.einnsyn.v1";

        ServiceRecord sr = new ServiceRecord(ServiceIdentifier.DPFIO, iso6523.getOrganizationIdentifier(), "pem123", kontoId);
        sr.setProcess(protocol);
        sr.setDocumentTypes(List.of(protocol));
        when(serviceRegistryLookup.getServiceRecord(any(), any())).thenReturn(sr);

        when(conversationService.registerStatus(anyString(), any(ReceiptStatus.class))).thenReturn(Optional.empty());

        StandardBusinessDocument sbd = mock(StandardBusinessDocument.class);
        when(sbd.getAny()).thenReturn(new FiksIoMessage().setSikkerhetsnivaa(3));
        when(sbd.getMessageId()).thenReturn(messageId);
        when(sbd.getProcess()).thenReturn(protocol);
        when(sbd.getReceiverIdentifier()).thenReturn(iso6523);
        when(sbd.getSenderIdentifier()).thenReturn(iso6523);
        when(sbd.getDocumentType()).thenReturn(protocol);

        NextMoveOutMessage msg = NextMoveOutMessage.of(sbd, ServiceIdentifier.DPFIO);

        StringPayload payload = new StringPayload("foo", "foo.txt");
        SendtMelding sentMsg = mock(SendtMelding.class);
        when(sentMsg.getMeldingId()).thenReturn(new MeldingId(UUID.fromString(messageId)));

        ArgumentCaptor<MeldingRequest> requestCaptor = ArgumentCaptor.forClass(MeldingRequest.class);
        when(fiksIOKlient.send(requestCaptor.capture(), any(List.class))).thenReturn(sentMsg);

        fiksIoService.createRequest(msg, List.of(payload));

        verify(fiksIOKlient).send(any(MeldingRequest.class), any(List.class));
        assertThat(requestCaptor.getValue().getMottakerKontoId().toString()).isEqualTo(kontoId);
        assertThat(requestCaptor.getValue().getMeldingType()).isEqualTo(protocol);
    }

}
