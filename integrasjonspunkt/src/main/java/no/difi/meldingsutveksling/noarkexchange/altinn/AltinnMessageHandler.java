package no.difi.meldingsutveksling.noarkexchange.altinn;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

public interface AltinnMessageHandler {

    void handleStandardBusinessDocument(StandardBusinessDocument sbd);
}
