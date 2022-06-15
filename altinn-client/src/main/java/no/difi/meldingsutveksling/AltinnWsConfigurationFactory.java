package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.AltinnFormidlingsTjenestenConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
@RequiredArgsConstructor
public class AltinnWsConfigurationFactory {

    private final IntegrasjonspunktProperties properties;

    public AltinnWsConfiguration create() {
        AltinnFormidlingsTjenestenConfig config = properties.getDpo();

        URL streamingserviceUrl = createUrl(config.getStreamingserviceUrl());
        URL brokerserviceUrl = createUrl(config.getBrokerserviceUrl());

        return AltinnWsConfiguration.builder()
                .username(config.getUsername())
                .password(config.getPassword())
                .orgnr(properties.getOrg().getIdentifier())
                .streamingServiceUrl(streamingserviceUrl)
                .brokerServiceUrl(brokerserviceUrl)
                .externalServiceCode(config.getServiceCode())
                .externalServiceEditionCode(Integer.parseInt(config.getServiceEditionCode()))
                .connectTimeout(config.getConnectTimeout())
                .requestTimeout(config.getRequestTimeout())
                .build();
    }

    private URL createUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new AltinnWsConfigurationException("The configured URL is invalid ", e);
        }
    }

    private static class AltinnWsConfigurationException extends RuntimeException {

        AltinnWsConfigurationException(String message, Exception e) {
            super(message, e);
        }
    }
}
