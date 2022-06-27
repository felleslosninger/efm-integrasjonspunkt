package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.shipping.ws.AltinnReasonFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@Component
public class AltinnConnectionCheck {

    private final AltinnWsClient altinnWsClient;
    private final IntegrasjonspunktProperties props;

    @PostConstruct
    public void checkTheConnection() {
        try {
            altinnWsClient.checkIfAvailableFiles(props.getOrg().getIdentifier().getPrimaryIdentifier());
            // TODO verify integrity of difi.move.dpo.reportees delegation
        } catch (IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage e) {
            throw new MeldingsUtvekslingRuntimeException("Could not check for available files from Altinn: " + AltinnReasonFactory.from(e), e);
        }
    }

}
