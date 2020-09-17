package no.difi.meldingsutveksling.noarkexchange.altinn;

import no.difi.meldingsutveksling.AltinnPackage;

public interface AltinnMessageHandler {

    void handleAltinnPackage(AltinnPackage altinnPackage);
}
