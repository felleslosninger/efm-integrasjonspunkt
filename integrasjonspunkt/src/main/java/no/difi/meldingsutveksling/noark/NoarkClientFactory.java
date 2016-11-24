package no.difi.meldingsutveksling.noark;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.mail.EduMailClient;
import no.difi.meldingsutveksling.noarkexchange.*;

import java.util.Optional;

/**
 * Factory to create NoarkClient for communicating correctly with specific Noark
 * systems: ie: P360, ePhorte, Akoz
 *
 * The archive systems does not have standardized WSDL interfaces
 */
public class NoarkClientFactory {

    private static final String E_PHORTE = "ephorte";
    private static final String P360 = "p360";
    private static final String WEBSAK = "websak";
    private static final String MAIL = "mail";
    private final NoarkClientSettings settings;

    public NoarkClientFactory(NoarkClientSettings settings) {
        this.settings = settings;
    }

    public NoarkClient from(IntegrasjonspunktProperties properties) throws UnknownArchiveSystemException {
        Optional<String> noarkType = Optional.of(properties).map(p -> p.getNoarkSystem()).map(n -> n.getType());
        if (!noarkType.isPresent()) {
            String message = "You need to configure what type of archive system you are using. Valid ones are %s, %s," +
                    " %s and %s";
            throw new MissingConfigurationException(String.format(message, E_PHORTE, P360, WEBSAK, MAIL));
        }

        switch (noarkType.get().toLowerCase()) {
            case E_PHORTE:
                return new EphorteClient(settings);
            case P360:
                return new P360Client(settings);
            case WEBSAK:
                return new WebsakClient(settings);
            case MAIL:
                return new EduMailClient(properties);
            default:
                throw new UnknownArchiveSystemException(noarkType.get());
        }

    }
}
