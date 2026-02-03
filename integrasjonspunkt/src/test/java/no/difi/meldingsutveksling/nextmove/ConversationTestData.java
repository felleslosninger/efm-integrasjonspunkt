package no.difi.meldingsutveksling.nextmove;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.Conversation;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import static no.difi.meldingsutveksling.nextmove.MessageStatusTestData.messageStatus;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

@UtilityClass
class ConversationTestData {

    static Conversation dpoConversation() {
        Conversation spy = spy(new Conversation()
                .setConversationId(UUID.randomUUID().toString())
                .setMessageId(UUID.randomUUID().toString())
                .setSenderIdentifier("0192:910077473")
                .setReceiverIdentifier("0192:910075918")
                .setProcessIdentifier("urn:no:difi:profile:arkivmelding:planByggOgGeodata:ver1.0")
                .setMessageReference("system-12463")
                .setMessageTitle("A message title")
                .setExternalSystemReference("reference from external system")
                .setPollable(true)
                .setFinished(false)
                .setExpiry(OffsetDateTime.parse("2019-04-25T12:38:23+01:00"))
                .setDirection(ConversationDirection.INCOMING)
                .setServiceIdentifier(ServiceIdentifier.DPO)
        );

        given(spy.getId()).willReturn(49L);
        given(spy.getLastUpdate()).willReturn(OffsetDateTime.parse("2019-03-25T12:38:23+01:00"));

        spy.setMessageStatuses(new HashSet<>(Collections.singletonList(messageStatus(76L, ReceiptStatus.INNKOMMENDE_MOTTATT, spy))));

        return spy;
    }

    static Conversation dpiConversation() {
        Conversation spy = spy(new Conversation()
                .setConversationId(UUID.randomUUID().toString())
                .setMessageId(UUID.randomUUID().toString())
                .setSenderIdentifier("0192:910077473")
                .setReceiverIdentifier("01017012345")
                .setProcessIdentifier("urn:no:difi:profile:digitalpost:info:ver1.0")
                .setMessageReference("system-98765")
                .setMessageTitle("A message title")
                .setExternalSystemReference("reference from external system")
                .setPollable(true)
                .setFinished(false)
                .setExpiry(OffsetDateTime.parse("2019-04-25T12:38:23+01:00"))
                .setDirection(ConversationDirection.INCOMING)
                .setServiceIdentifier(ServiceIdentifier.DPI)
        );

        given(spy.getId()).willReturn(56L);
        given(spy.getLastUpdate()).willReturn(OffsetDateTime.parse("2019-03-25T12:38:23+01:00"));

        spy.setMessageStatuses(new HashSet<>(Collections.singletonList(messageStatus(76L, ReceiptStatus.INNKOMMENDE_MOTTATT, spy))));

        return spy;
    }
}
