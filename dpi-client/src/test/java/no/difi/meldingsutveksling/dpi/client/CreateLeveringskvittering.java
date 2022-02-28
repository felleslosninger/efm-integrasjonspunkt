package no.difi.meldingsutveksling.dpi.client;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Kvittering;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Leveringskvittering;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.MessageType;

import java.time.Clock;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CreateLeveringskvittering implements ReceiptFactory {

    private final Clock clock;

    @Override
    public MessageType getMessageType() {
        return MessageType.LEVERINGSKVITTERING;
    }

    @Override
    public Kvittering getReceipt(ReceiptInput input) {
        Leveringskvittering kvittering = new Leveringskvittering();
        kvittering.setMottaker(input.getMottaker());
        kvittering.setAvsender(input.getAvsender());
        kvittering.setTidspunkt(OffsetDateTime.now(clock));
        return kvittering;
    }
}
