package no.difi.meldingsutveksling.noarkexchange.altinn;

import no.difi.meldingsutveksling.altinnv3.DPO.altinn2.AltinnPackage;

import java.io.IOException;

public interface AltinnMessageHandler {

    void handleAltinnPackage(AltinnPackage altinnPackage) throws IOException;

}
