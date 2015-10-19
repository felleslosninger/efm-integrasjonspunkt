package no.difi.meldingsutveksling.noark;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfig;
import no.difi.meldingsutveksling.noarkexchange.*;

/**
 * Factory to create NoarkClient for communicating correctly with specific Noark systems: ie: P360, ePhorte, Akoz
 *
 * The archive systems does not have standardized WSDL interfaces
 */
public class NoarkClientFactory {

    private static final String E_PHORTE = "ePhorte";
    private static final String P360 = "P360";
    private final NoarkClientSettings settings;

    public NoarkClientFactory(NoarkClientSettings settings) {
        this.settings = settings;
    }

    public NoarkClient from(IntegrasjonspunktConfig config) throws UnknownArchiveSystemException {
        String noarkType = config.getNoarkType();
        if(noarkType == null) {
            String message = "You need to configure what type of archive system you are using. Valid ones are %s, %s";
            throw new MissingConfigurationException(String.format(message, E_PHORTE, P360));
        }

        WebServiceTemplateFactory templateFactory = settings.createTemplateFactory();

        if(E_PHORTE.equals(noarkType)) {
            return new EphorteClient(settings);
        } else if(P360.equals(noarkType)) {
            return new P360Client(settings, templateFactory);
        } else {
            throw new UnknownArchiveSystemException(noarkType);
        }
    }
}
