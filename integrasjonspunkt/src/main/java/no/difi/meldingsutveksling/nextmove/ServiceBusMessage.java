package no.difi.meldingsutveksling.nextmove;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

@Data
@RequiredArgsConstructor(staticName = "of")
@Builder
public class ServiceBusMessage {

    private final StandardBusinessDocument body;
    private final String lockToken;
    private final String messageId;
    private final String sequenceNumber;
}
