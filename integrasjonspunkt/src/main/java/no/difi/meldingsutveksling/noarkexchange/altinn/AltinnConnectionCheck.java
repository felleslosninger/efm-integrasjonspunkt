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
import java.util.Set;

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
            altinnWsClient.checkIfAvailableFiles(props.getOrg().getNumber());
        } catch (IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage e) {
            throw new MeldingsUtvekslingRuntimeException("Could not check for available files from Altinn: " + AltinnReasonFactory.from(e), e);
        }

        verifyIntegrityOfDPODelegationForOnBehalfOfReportees();
    }

    private void verifyIntegrityOfDPODelegationForOnBehalfOfReportees() {
        Set<String> reportees = props.getDpo().getReportees();
        if (reportees != null && !reportees.isEmpty()) {
            for (String reportee : reportees) {
                try {
                    altinnWsClient.checkIfAvailableFiles(reportee);
                    log.info("Altinn DPO delegation verified for reportee: {}", reportee);
                } catch (IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage e) {
                    throw new RuntimeException("Failed to verify DPO delegation for reportee " + reportee, e);
                }
            }
        } else {
            log.info("No DPO reportees configured – skipping delegation check.");
        }
    }
}

