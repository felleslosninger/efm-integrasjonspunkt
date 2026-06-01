package no.difi.meldingsutveksling.dph.client;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.dph.client.domain.ApplicationReceiptResponse;
import no.difi.meldingsutveksling.dph.client.domain.BusinessDocumentResponse;
import no.difi.meldingsutveksling.dph.client.domain.SendApplicationReceiptInput;
import no.difi.meldingsutveksling.dph.client.domain.SendBusinessDocumentInput;
import no.difi.meldingsutveksling.dph.client.internal.DphClient;
import no.difi.meldingsutveksling.dph.client.internal.DphDocumentConverter;
import no.difi.meldingsutveksling.dph.client.internal.DphParcelService;
import no.difi.meldingsutveksling.dph.client.internal.WrappedPackage;
import no.difi.meldingsutveksling.nextmove.DialogmeldingKvitteringMessage;
import no.difi.meldingsutveksling.nextmove.DialogmeldingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingApplicationReceipt;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingBusinessDocument;
import no.difi.meldingsutveksling.nhn.adapter.model.IncomingMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.MessageStatus;
import no.difi.meldingsutveksling.nhn.adapter.model.OutgoingApplicationReceipt;
import no.difi.meldingsutveksling.nhn.adapter.model.OutgoingBusinessDocument;
import no.difi.meldingsutveksling.nhn.adapter.model.serialization.KxJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DphClientServiceTest {

    @Mock
    private DphClient dphClient;
    @Mock
    private DphParcelService parcelService;
    @Mock
    private DphDocumentConverter dphDocumentConverter;

    @InjectMocks
    private DphClientService dphClientService;

    private final Iso6523 onBehalfOf = Iso6523.parse("0192:987654321");

    @Test
    void testGetStatus() {
        String messageId = "msg-id";
        List<MessageStatus> statuses = Collections.emptyList();
        given(dphClient.getStatus(onBehalfOf, messageId)).willReturn(statuses);

        List<MessageStatus> result = dphClientService.getStatus(onBehalfOf, messageId);

        assertThat(result).isSameAs(statuses);
        verify(dphClient).getStatus(onBehalfOf, messageId);
    }

    @Test
    void testSendBusinessDocument() {
        SendBusinessDocumentInput input = mock(SendBusinessDocumentInput.class);
        OutgoingBusinessDocument external = mock(OutgoingBusinessDocument.class);
        given(dphDocumentConverter.toExternal(input)).willReturn(external);
        given(input.getEncryptedAsic()).willReturn(new ByteArrayResource("asic".getBytes()));

        UUID expectedId = UUID.randomUUID();

        try (MockedStatic<KxJson> kxJsonMockedStatic = mockStatic(KxJson.class)) {
            kxJsonMockedStatic.when(() -> KxJson.encode(eq(external), any())).thenReturn("encoded-json");
            given(parcelService.signAndEncrypt("encoded-json")).willReturn("signed-encrypted");
            given(dphClient.sendBusinessDocument(eq(onBehalfOf), any(WrappedPackage.class))).willReturn(expectedId);

            UUID result = dphClientService.sendBusinessDocument(onBehalfOf, input);

            assertThat(result).isEqualTo(expectedId);
            verify(dphClient).sendBusinessDocument(eq(onBehalfOf), argThat(wp ->
                "signed-encrypted".equals(wp.forretningsmelding()) && wp.encryptedAsic() != null
            ));
        }
    }

    @Test
    void testSendApplicationReceipt() {
        SendApplicationReceiptInput input = mock(SendApplicationReceiptInput.class);
        OutgoingApplicationReceipt external = mock(OutgoingApplicationReceipt.class);
        given(dphDocumentConverter.toExternal(input)).willReturn(external);

        UUID expectedId = UUID.randomUUID();

        try (MockedStatic<KxJson> kxJsonMockedStatic = mockStatic(KxJson.class)) {
            kxJsonMockedStatic.when(() -> KxJson.encode(eq(external), any())).thenReturn("encoded-json");
            given(parcelService.signAndEncrypt("encoded-json")).willReturn("signed-encrypted");
            given(dphClient.sendApplicationReceipt(eq(onBehalfOf), any(WrappedPackage.class))).willReturn(expectedId);

            UUID result = dphClientService.sendApplicationReceipt(onBehalfOf, input);

            assertThat(result).isEqualTo(expectedId);
            verify(dphClient).sendApplicationReceipt(eq(onBehalfOf), argThat(wp ->
                "signed-encrypted".equals(wp.forretningsmelding()) && wp.encryptedAsic() == null
            ));
        }
    }

    @Test
    void testGetMessages() {
        Integer receiverHerId = 12345;
        List<IncomingMessage> messages = Collections.emptyList();
        given(dphClient.getMessages(onBehalfOf, receiverHerId)).willReturn(messages);

        List<IncomingMessage> result = dphClientService.getMessages(onBehalfOf, receiverHerId);

        assertThat(result).isSameAs(messages);
        verify(dphClient).getMessages(onBehalfOf, receiverHerId);
    }

    @Test
    void testReceiveApplicationReceipt() {
        String id = "msg-id";
        WrappedPackage wrappedPackage = new WrappedPackage("encrypted-json");
        given(parcelService.signAndEncrypt(any())).willReturn("mocked-token");
        given(dphClient.receiveApplicationReceipt(onBehalfOf, "mocked-token")).willReturn(wrappedPackage);
        JWSObject jwsObject = mock(JWSObject.class);
        given(jwsObject.getPayload()).willReturn(new Payload("decrypted-json"));
        given(parcelService.decryptAndVerify("encrypted-json")).willReturn(jwsObject);

        IncomingApplicationReceipt incomingAppReceipt = mock(IncomingApplicationReceipt.class);
        given(incomingAppReceipt.getId()).willReturn("msg-uuid");
        given(incomingAppReceipt.getRawReceipt()).willReturn("raw-receipt");
        given(incomingAppReceipt.getPayload()).willReturn(mock(no.difi.meldingsutveksling.nhn.adapter.model.DialogmeldingKvitteringMessage.class));

        DialogmeldingKvitteringMessage internalPayload = mock(DialogmeldingKvitteringMessage.class);
        given(dphDocumentConverter.toInternal(any(no.difi.meldingsutveksling.nhn.adapter.model.DialogmeldingKvitteringMessage.class)))
            .willReturn(internalPayload);

        try (MockedStatic<KxJson> kxJsonMockedStatic = mockStatic(KxJson.class)) {
            kxJsonMockedStatic.when(() -> KxJson.decode(eq("decrypted-json"), any())).thenReturn(incomingAppReceipt);

            ApplicationReceiptResponse result = dphClientService.receiveApplicationReceipt(onBehalfOf, id);

            assertThat(result.getMessageId()).isEqualTo("msg-uuid");
            assertThat(result.getRawReceipt()).isEqualTo("raw-receipt");
            assertThat(result.getPayload()).isSameAs(internalPayload);
        }
    }

    @Test
    void testReceiveBusinessDocument() {
        String id = "msg-id";
        ByteArrayResource asic = new ByteArrayResource("asic".getBytes());
        WrappedPackage wrappedPackage = new WrappedPackage("encrypted-json", asic);
        given(parcelService.signAndEncrypt(any())).willReturn("mocked-token");
        given(dphClient.receiveBusinessDocument(onBehalfOf, "mocked-token")).willReturn(wrappedPackage);
        JWSObject jwsObject = mock(JWSObject.class);
        given(jwsObject.getPayload()).willReturn(new Payload("decrypted-json"));
        given(parcelService.decryptAndVerify("encrypted-json")).willReturn(jwsObject);

        IncomingBusinessDocument incomingDoc = mock(IncomingBusinessDocument.class);
        given(incomingDoc.getId()).willReturn("msg-uuid");
        given(incomingDoc.getSenderHerId()).willReturn(1);
        given(incomingDoc.getReceiverHerId()).willReturn(2);
        given(incomingDoc.getConversationId()).willReturn("conv-id");
        given(incomingDoc.getPayload()).willReturn(mock(no.difi.meldingsutveksling.nhn.adapter.model.DialogmeldingMessage.class));

        DialogmeldingMessage internalPayload = mock(DialogmeldingMessage.class);
        given(dphDocumentConverter.toInternal(any(no.difi.meldingsutveksling.nhn.adapter.model.DialogmeldingMessage.class)))
            .willReturn(internalPayload);

        try (MockedStatic<KxJson> kxJsonMockedStatic = mockStatic(KxJson.class)) {
            kxJsonMockedStatic.when(() -> KxJson.decode(eq("decrypted-json"), any())).thenReturn(incomingDoc);

            BusinessDocumentResponse result = dphClientService.receiveBusinessDocument(onBehalfOf, id);

            assertThat(result.getMessageId()).isEqualTo("msg-uuid");
            assertThat(result.getSenderHerId()).isEqualTo(1);
            assertThat(result.getReceiverHerId()).isEqualTo(2);
            assertThat(result.getConversationId()).isEqualTo("conv-id");
            assertThat(result.getPayload()).isSameAs(internalPayload);
            assertThat(result.getEncryptedAsic()).isSameAs(asic);
        }
    }

    @Test
    void testMarkAsRead() {
        Integer receiverHerId = 12345;
        String messageId = "msg-id";

        dphClientService.markAsRead(onBehalfOf, receiverHerId, messageId);

        verify(dphClient).markAsRead(onBehalfOf, receiverHerId, messageId);
    }

    @Test
    void testGetMaskinportenToken() {
        String expectedToken = "token";
        given(dphClient.getMaskinportenToken(onBehalfOf)).willReturn(expectedToken);

        String result = dphClientService.getMaskinportenToken(onBehalfOf);

        assertThat(result).isEqualTo(expectedToken);
        verify(dphClient).getMaskinportenToken(onBehalfOf);
    }
}
