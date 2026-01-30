package no.difi.meldingsutveksling.altinnv3.dpv;


import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;

@RequiredArgsConstructor
@Slf4j
public class CorrespondenceAgencyConnectionCheck {

    private final CorrespondenceApiClient correspondenceApiClient;
    private final IntegrasjonspunktProperties integrasjonspunktProperties;

    @PostConstruct
    public void checkTheConnection() {
        try {
            if(!integrasjonspunktProperties.getDpv().getHealthCheckUrl().isEmpty()){
                correspondenceApiClient.connectionTest();
            }else {
                log.info("No connection check done, healthCheckUrl was not set in configuration");
            }
        } catch (Exception e) {
            throw new CorrespondenceApiException("Couldn't connect to Altinn while doing connection check", e);
        }
    }

}
