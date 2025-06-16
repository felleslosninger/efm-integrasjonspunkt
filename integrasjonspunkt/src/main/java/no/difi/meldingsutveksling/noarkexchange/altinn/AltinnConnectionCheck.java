package no.difi.meldingsutveksling.noarkexchange.altinn;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@Component
public class AltinnConnectionCheck {

//    private final AltinnWsClient altinnWsClient;
    private final IntegrasjonspunktProperties props;

    @PostConstruct
    public void checkTheConnection() {
        try {
            // FIXME gjør en "ping" sjekk for å se om det er mulig å snakke med AltInn v3
            //altinnWsClient.checkIfAvailableFiles(props.getOrg().getNumber());
            // TODO verify integrity of difi.move.dpo.reportees delegation
        } catch (Exception e) {
            throw new MeldingsUtvekslingRuntimeException("Could not connect to Altinn", e);
        }
    }

}
