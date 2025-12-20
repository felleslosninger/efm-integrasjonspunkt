package no.difi.meldingsutveksling.noarkexchange.altinn;

import com.google.common.collect.Sets;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.dpo.BrokerApiClient;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@Component
public class AltinnConnectionCheck {

    private final IntegrasjonspunktProperties properties;
    private final BrokerApiClient brokerApiClient;

    @PostConstruct
    public void checkTheConnection() {

// FIXME denne sjekken stopper oppstart av IP om ikke alt er på plass, dermed virker ikke onboarding som den skal
//        var systemUsers = Sets.newHashSet(properties.getDpo().getSystemUser());
//        systemUsers.addAll(properties.getDpo().getReportees());
//
//        systemUsers.forEach(systemUser -> {
//            try {
//                brokerApiClient.getAvailableFiles(systemUser);
//            }
//            catch (Exception e) {
//                if(e instanceof HttpClientErrorException ex && ex.getStatusCode() == HttpStatus.NOT_FOUND) {
//                    throw new MeldingsUtvekslingRuntimeException("Could not connect to Altinn, got 404 not found, systemuser " + systemUser.getName() + " might not exist", e);
//                }
//                throw new MeldingsUtvekslingRuntimeException("Could not connect to Altinn with systemUser.orgId: " + systemUser.getOrgId() + " systemUser.name: " + systemUser.getName(), e);
//            }
//        });

    }
}
