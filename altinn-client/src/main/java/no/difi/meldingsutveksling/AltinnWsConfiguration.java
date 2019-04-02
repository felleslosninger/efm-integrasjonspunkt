package no.difi.meldingsutveksling;

import lombok.Builder;
import lombok.Value;

import java.net.URL;

@Value
@Builder
public class AltinnWsConfiguration {

    private URL streamingServiceUrl;
    private URL brokerServiceUrl;
    private String username;
    private String password;
    private String externalServiceCode;
    private int externalServiceEditionCode;
}
