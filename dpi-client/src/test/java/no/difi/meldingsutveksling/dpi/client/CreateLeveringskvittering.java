package no.difi.meldingsutveksling.dpi.client;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Kvittering;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Leveringskvittering;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.MessageType;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class CreateLeveringskvittering implements no.difi.meldingsutveksling.dpi.client.ReceiptFactory {

    private final Clock clock;

    @Override
    public MessageType getMessageType() {
        return MessageType.LEVERINGSKVITTERING;
    }

    @Override
    public Kvittering getReceipt(no.difi.meldingsutveksling.dpi.client.ReceiptInput input) {
        Leveringskvittering kvittering = new Leveringskvittering();
        kvittering.setMottaker(input.getMottaker());
        kvittering.setAvsender(input.getAvsender());
        kvittering.setTidspunkt(OffsetDateTime.now(clock));
        return kvittering;
    }
}
