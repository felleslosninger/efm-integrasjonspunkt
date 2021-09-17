package no.difi.meldingsutveksling.dpi.json;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentUtils;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.digdir.dpi.client.domain.messagetypes.Kvittering;
import no.digdir.dpi.client.domain.messagetypes.MessageType;

import static no.difi.meldingsutveksling.receipt.ReceiptStatus.FEIL;

@RequiredArgsConstructor
public class JsonDpiReceiptMapper {

    private final MessageStatusFactory messageStatusFactory;

    public MessageStatus from(StandardBusinessDocument standardBusinessDocument) {
        MessageStatus ms = StandardBusinessDocumentUtils.getType(standardBusinessDocument)
                .map(MessageType::fromType)
                .map(this::getMessageStatus)
                .orElseGet(() -> messageStatusFactory.getMessageStatus(ReceiptStatus.ANNET, "Ukjent kvittering"));

        standardBusinessDocument.getBusinessMessage(Kvittering.class)
                .filter(kvittering -> kvittering.getTidspunkt() != null)
                .ifPresent(kvittering -> ms.setLastUpdate(kvittering.getTidspunkt()));

        return ms;
    }

    private MessageStatus getMessageStatus(MessageType messageType) {
        switch (messageType) {
            case LEVERINGSKVITTERING:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEVERT, "Kvittering på at digital post er tilgjengeliggjort eller at en fysisk post er postlagt");
            case AAPNINGSKVITTERING:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEST, "Kvittering fra Innbygger for at digital post er åpnet");
            case VARSLINGFEILETKVITTERING:
                return messageStatusFactory.getMessageStatus(FEIL, "Kvittering for at en spesifisert varsling ikke har blitt sendt");
            case MOTTAKSKVITTERING:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.MOTTATT, "Kvittering fra utskrift og forsendelsestjenesten om at melding er mottatt og lagt til print");
            case RETURPOSTKVITTERING:
                return messageStatusFactory.getMessageStatus(FEIL, "Kvittering fra utskrift og forsendelsestjenesten om at posten ikke har blitt levert til Mottaker.");
            case FEIL:
                return messageStatusFactory.getMessageStatus(FEIL, "Generell melding om at det har skjedd en feil");
            default:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.ANNET, "Ukjent kvittering");
        }
    }
}
