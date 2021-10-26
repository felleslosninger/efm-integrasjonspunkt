package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Data;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

@Data
public class ReceivedMessage {

    private no.difi.meldingsutveksling.dpi.client.domain.Message message;
    private StandardBusinessDocument standardBusinessDocument;
}
