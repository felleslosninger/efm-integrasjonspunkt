package no.difi.meldingsutveksling.nextmove;

import java.util.Collections;
import java.util.Map;

public interface DpiMessage {

    String getAvsenderId();

    String getFakturaReferanse();

    default Map<String, String> getMetadataFiler() {
        return Collections.emptyMap();
    }
}
