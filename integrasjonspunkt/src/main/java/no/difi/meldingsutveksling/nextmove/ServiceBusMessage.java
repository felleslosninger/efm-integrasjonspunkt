package no.difi.meldingsutveksling.nextmove;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusPayload;

@Value
@RequiredArgsConstructor(staticName = "of")
@Builder
public class ServiceBusMessage {

    private final ServiceBusPayload payload;
    private final String lockToken;
    private final String messageId;
    private final String sequenceNumber;
}
