package no.difi.meldingsutveksling.noarkexchange.altinn;

import no.difi.meldingsutveksling.AltinnPackage;

import java.io.IOException;

public interface AltinnMessageHandler {

    void handleAltinnPackage(AltinnPackage altinnPackage) throws IOException;
}
