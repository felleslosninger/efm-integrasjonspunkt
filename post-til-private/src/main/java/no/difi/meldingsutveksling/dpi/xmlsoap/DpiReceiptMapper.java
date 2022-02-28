package no.difi.meldingsutveksling.dpi.xmlsoap;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.difi.sdp.client2.domain.kvittering.ForretningsKvittering;

import java.time.Clock;
import java.time.OffsetDateTime;

import static no.difi.meldingsutveksling.receipt.ReceiptStatus.FEIL;

@RequiredArgsConstructor
public class DpiReceiptMapper {

    private final MessageStatusFactory messageStatusFactory;
    private final Clock clock;

    public MessageStatus from(ForretningsKvittering forretningsKvittering) {
        MessageStatus ms = getMessageStatus(forretningsKvittering.getClass());
        if (forretningsKvittering.getTidspunkt() != null) {
            ms.setLastUpdate(OffsetDateTime.ofInstant(forretningsKvittering.getTidspunkt(), clock.getZone()));
        }
        return ms;
    }

    private MessageStatus getMessageStatus(Class clazz) {
        if (clazz == null) {
            return messageStatusFactory.getMessageStatus(ReceiptStatus.ANNET, "Ukjent kvittering");
        }

        switch (clazz.getSimpleName()) {
            case "LeveringsKvittering":
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEVERT, "Kvittering på at digital post er tilgjengeliggjort eller at en fysisk post er postlagt");
            case "AapningsKvittering":
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEST, "Kvittering fra Innbygger for at digital post er åpnet");
            case "VarslingFeiletKvittering":
                return messageStatusFactory.getMessageStatus(FEIL, "Kvittering for at en spesifisert varsling ikke har blitt sendt");
            case "MottaksKvittering":
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEVERT, "Kvittering fra utskrift og forsendelsestjenesten om at melding er mottatt og lagt til print");
            case "ReturpostKvittering":
                return messageStatusFactory.getMessageStatus(FEIL, "Kvittering fra utskrift og forsendelsestjenesten om at posten ikke har blitt levert til Mottaker.");
            case "Feil":
                return messageStatusFactory.getMessageStatus(FEIL, "Generell melding om at det har skjedd en feil");
            default:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.ANNET, "Ukjent kvittering");
        }
    }

    MessageStatus getEmpty() {
        return messageStatusFactory.getMessageStatus(ReceiptStatus.ANNET, "Tom kvittering");
    }
}
