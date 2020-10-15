package no.difi.meldingsutveksling.nextmove.servicebus;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(staticName = "of")
@Builder
public class ServiceBusMessage {

    private final ServiceBusPayload payload;
    private final String lockToken;
    private final String messageId;
    private final String sequenceNumber;
}
