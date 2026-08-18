package no.difi.meldingsutveksling.ks.svarut.ws;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.status.MessageStatus;
import no.difi.meldingsutveksling.status.MessageStatusFactory;

import java.util.Objects;

@RequiredArgsConstructor
class FiksWebServiceStatusMapper {

    private final MessageStatusFactory messageStatusFactory;

    public MessageStatus mapFrom(ForsendelseStatus forsendelseStatus) {
        if (Objects.requireNonNull(forsendelseStatus) == ForsendelseStatus.LEST) {
            // SvarUt garanterer leveranse etter ok mottak av melding.
            // LEVERT registreres derfor når melding blir sendt.
            // Ut over dette er det bare LEST som er relevant å hente inn.
            return messageStatusFactory.getMessageStatus(ReceiptStatus.LEST);
        }
        return null;
    }
}
