package no.difi.meldingsutveksling.dpi.client.domain;

import lombok.Data;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

@Data
public class ReceivedMessage {

    private Message message;
    private StandardBusinessDocument standardBusinessDocument;
}
