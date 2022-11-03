package no.difi.meldingsutveksling.ks.mapping;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ks.svarut.ForsendelseStatus;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FiksStatusMapper {

    private final MessageStatusFactory messageStatusFactory;

    public MessageStatus mapFrom(ForsendelseStatus forsendelseStatus) {
        switch (forsendelseStatus) {
            case LEST:
                // SvarUt garanterer leveranse etter ok mottak av melding.
                // LEVERT registreres derfor når melding blir sendt.
                // Ut over dette er det bare LEST som er relevant å hente inn.
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEST);
            default:
                return null;
        }
    }

    public MessageStatus noForsendelseId() {
        return messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL, "forsendelseId finnes ikke i SvarUt.");
    }
}
