package no.difi.meldingsutveksling.cucumber;

import lombok.Value;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

@Value
public class Message {

    private final StandardBusinessDocument sbd;
    private final byte[] asic;
}
