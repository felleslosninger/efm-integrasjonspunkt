package no.difi.meldingsutveksling.status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.ExecutionException;

/**
 * Periodically checks  for DPI receipts.
 */
@RequiredArgsConstructor
@Slf4j
public class DpiStatusPolling {

    private final IntegrasjonspunktProperties props;
    private final DpiReceiptService dpiReceiptService;

    @Scheduled(fixedRate = 10000)
    public void dpiReceiptsScheduledTask() throws ExecutionException, InterruptedException {
        if (props.getFeature().isEnableReceipts() && props.getFeature().isEnableDPI()) {
            dpiReceiptService.handleReceipts();
        }
    }
}
