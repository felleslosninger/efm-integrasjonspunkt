package no.difi.meldingsutveksling.noark;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.P360Client;

/**
 * Factory to create NoarkClient for communicating correctly with specific Noark systems: ie: P360, ePhorte, Akoz
 *
 * The archive systems does not have standardized WSDL interfaces
 */
public class NoarkClientFactory {

    NoarkClient from(IntegrasjonspunktConfig config) {
        return new P360Client();
    }
}
