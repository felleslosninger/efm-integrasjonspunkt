package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.sdp.client2.domain.kvittering.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;

import static no.difi.meldingsutveksling.receipt.ReceiptStatus.FEIL;

public class DpiReceiptMapper {

    private static final HashMap<Class, MessageStatus> mapper;
    static {
        mapper = new HashMap<>();
        mapper.put(LeveringsKvittering.class, MessageStatus.of(ReceiptStatus.LEVERT, "Kvittering på at digital post er tilgjengeliggjort eller at en fysisk post er postlagt"));
        mapper.put(AapningsKvittering.class, MessageStatus.of(ReceiptStatus.LEST, "Kvittering fra Innbygger for at digital post er åpnet"));
        mapper.put(VarslingFeiletKvittering.class, MessageStatus.of(FEIL, "Kvittering for at en spesifisert varsling ikke har blitt sendt"));
        mapper.put(MottaksKvittering.class, MessageStatus.of(ReceiptStatus.LEST, "Kvittering fra utskrift og forsendelsestjenesten om at melding er mottatt og lagt til print"));
        mapper.put(ReturpostKvittering.class, MessageStatus.of(FEIL, "Kvittering fra utskrift og forsendelsestjenesten om at posten ikke har blitt levert til Mottaker."));
        mapper.put(Feil.class, MessageStatus.of(FEIL, "Generell melding om at det har skjedd en feil"));
    }

    public static MessageStatus from(ForretningsKvittering forretningsKvittering) {
        MessageStatus ms = mapper.getOrDefault(forretningsKvittering.getClass(), MessageStatus.of(ReceiptStatus.ANNET, "Ukjent kvittering"));
        if (forretningsKvittering.getTidspunkt() != null) {
            ms.setLastUpdate(LocalDateTime.ofInstant(forretningsKvittering.getTidspunkt(), ZoneId.systemDefault()));
        }
        return ms;
    }

}
