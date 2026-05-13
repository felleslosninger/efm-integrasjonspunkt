package no.difi.meldingsutveksling.altinnv3.dpo;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
public class ReporteesConnectionCheck {
    private final IntegrasjonspunktProperties properties;
    private final AltinnDPODownloadService altinnDownloadService;

    @PostConstruct
    public void reporteesConnectionCheck() {
        var reporteesSystemUsers = properties.getDpo().getReportees();

        if(!reporteesSystemUsers.isEmpty()) {
            log.info("Performing a connection test for reportees defined in properties by polling for DPO messages");
            reporteesSystemUsers.forEach(system -> {
                log.info("Connection test for systemUser:{}", system);
                try {
                    altinnDownloadService.getAvailableFiles(system);
                } catch (Exception e) {
                    throw new RuntimeException(
                        "Failed to poll messages from Altinn for system: " + system + ". The system user configuration might be wrong", e);
                }
            });
        }
    }
}
