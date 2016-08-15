package no.difi.meldingsutveksling.noark;

import no.difi.meldingsutveksling.config.IntegrasjonspunktConfiguration;
import no.difi.meldingsutveksling.noarkexchange.*;

/**
 * Factory to create NoarkClient for communicating correctly with specific Noark systems: ie: P360, ePhorte, Akoz
 *
 * The archive systems does not have standardized WSDL interfaces
 */
public class NoarkClientFactory {

    private static final String E_PHORTE = "ePhorte";
    private static final String P360 = "P360";
    private static final String WEBSAK = "WebSak";
    private final NoarkClientSettings settings;

    public NoarkClientFactory(NoarkClientSettings settings) {
        this.settings = settings;
    }

    public NoarkClient from(IntegrasjonspunktConfiguration config) throws UnknownArchiveSystemException {
        String noarkType = config.getNoarkType();
        if(noarkType == null) {
            String message = "You need to configure what type of archive system you are using. Valid ones are %s, %s";
            throw new MissingConfigurationException(String.format(message, E_PHORTE, P360));
        }

        WebServiceTemplateFactory templateFactory = settings.createTemplateFactory();

        if (E_PHORTE.equalsIgnoreCase(noarkType)) {
            return new EphorteClient(settings);
        } else if (P360.equalsIgnoreCase(noarkType)) {
            return new P360Client(settings);
        } else if (WEBSAK.equalsIgnoreCase(noarkType)) {
            return new WebsakClient(settings);
        } else {
            throw new UnknownArchiveSystemException(noarkType);
        }
    }
}
