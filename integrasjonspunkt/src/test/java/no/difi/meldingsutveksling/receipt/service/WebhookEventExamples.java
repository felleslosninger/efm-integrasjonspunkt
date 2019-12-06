package no.difi.meldingsutveksling.receipt.service;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.webhooks.event.MessageStatusContent;
import no.difi.meldingsutveksling.webhooks.event.PingContent;

import java.time.OffsetDateTime;
import java.util.UUID;

@UtilityClass
public class WebhookEventExamples {

    static PingContent ping() {
        return new PingContent()
                .setCreatedTs(OffsetDateTime.parse("2019-03-25T12:38:23+01:00"))
                .setEvent("ping");
    }

    static MessageStatusContent messageStatus() {
        return new MessageStatusContent()
                .setCreatedTs(OffsetDateTime.parse("2019-03-25T12:38:23+01:00"))
                .setResource("messages")
                .setEvent("status")
                .setMessageId(UUID.randomUUID().toString())
                .setConversationId(UUID.randomUUID().toString())
                .setDirection(ConversationDirection.INCOMING)
                .setServiceIdentifier(ServiceIdentifier.DPO)
                .setStatus(ReceiptStatus.LEVETID_UTLOPT.name())
                .setDescription("Levetiden for meldingen er utgått. Må sendes på nytt");
    }
}
