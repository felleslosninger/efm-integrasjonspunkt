package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.AltinnFormidlingsTjenestenConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;

@Component
@RequiredArgsConstructor
public class AltinnWsConfigurationFactory {

    private final IntegrasjonspunktProperties properties;

    public AltinnWsConfiguration fromServiceRecord(ServiceRecord serviceRecord) {
        AltinnFormidlingsTjenestenConfig config = properties.getDpo();
        URL streamingserviceUrl = createUrl(serviceRecord.getEndPointURL() + config.getStreamingserviceUrl());
        URL brokerserviceUrl = createUrl(serviceRecord.getEndPointURL() + config.getBrokerserviceUrl());

        return AltinnWsConfiguration.builder()
                .username(config.getUsername())
                .password(config.getPassword())
                .streamingServiceUrl(streamingserviceUrl)
                .brokerServiceUrl(brokerserviceUrl)
                .externalServiceCode(serviceRecord.getServiceCode())
                .externalServiceEditionCode(Integer.valueOf(serviceRecord.getServiceEditionCode()))
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
