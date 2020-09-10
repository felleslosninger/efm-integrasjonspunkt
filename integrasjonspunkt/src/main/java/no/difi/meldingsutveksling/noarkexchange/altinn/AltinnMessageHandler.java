package no.difi.meldingsutveksling.noarkexchange.altinn;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import java.io.InputStream;

public interface AltinnMessageHandler {

    void handleStandardBusinessDocument(StandardBusinessDocument sbd, InputStream asicStream);
}
