package no.difi.meldingsutveksling.noarkexchange;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.mail.MailClient;

import java.util.Optional;

/**
 * Factory to create NoarkClient for communicating correctly with specific Noark
 * systems: ie: P360, ePhorte, Akoz
 * <p>
 * The archive systems does not have standardized WSDL interfaces
 */
@RequiredArgsConstructor
public class NoarkClientFactory {

    private static final String E_PHORTE = "ephorte";
    private static final String P360 = "p360";
    private static final String WEBSAK = "websak";
    private static final String MAIL = "mail";

    private final NoarkClientSettings settings;

    public NoarkClient from(IntegrasjonspunktProperties properties) {
        String noarkType = Optional.of(properties)
                .map(IntegrasjonspunktProperties::getNoarkSystem)
                .map(IntegrasjonspunktProperties.NorskArkivstandardSystem::getType)
                .orElseThrow(() -> new MissingConfigurationException(
                        String.format("You need to configure what type of archive system you are using. "
                                + "Valid ones are %s, %s, %s and %s", E_PHORTE, P360, WEBSAK, MAIL)));

        switch (noarkType.toLowerCase()) {
            case E_PHORTE:
                return new EphorteClient(settings);
            case P360:
                return new P360Client(settings);
            case WEBSAK:
                return new WebsakClient(settings);
            case MAIL:
                return new MailClient(properties);
            default:
                throw new UnknownArchiveSystemException(noarkType);
        }
    }
}
