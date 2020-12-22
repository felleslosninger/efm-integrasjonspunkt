package no.difi.meldingsutveksling;

import lombok.Builder;
import lombok.Value;

import java.net.URL;

@Value
@Builder
public class AltinnWsConfiguration {

    URL streamingServiceUrl;
    URL brokerServiceUrl;
    String orgnr;
    String username;
    String password;
    String externalServiceCode;
    int externalServiceEditionCode;
    Integer connectTimeout;
    Integer requestTimeout;
}
