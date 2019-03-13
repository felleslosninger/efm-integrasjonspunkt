package no.difi.meldingsutveksling.cucumber;

import lombok.Value;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import java.io.InputStream;

@Value
class TransportInput {

    private final StandardBusinessDocument sbd;
    private final InputStream inputStream;
}
