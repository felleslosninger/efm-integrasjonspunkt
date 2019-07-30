package no.difi.meldingsutveksling.nextmove.servicebus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ServiceBusPayload {
    private StandardBusinessDocument sbd;
    private byte[] asic;
}
