package no.difi.meldingsutveksling.status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Periodically checks  for DPI receipts.
 */
@RequiredArgsConstructor
@Slf4j
public class DpiStatusPolling {

    private final IntegrasjonspunktProperties props;
    private final DpiReceiptService dpiReceiptService;

    @Scheduled(fixedRate = 10000)
    public void dpiReceiptsScheduledTask() throws InterruptedException, ExecutionException {
        if (props.getFeature().isEnableReceipts() && props.getFeature().isEnableDPI()) {
            int mpcConcurrency = props.getDpi().getMpcConcurrency();
            List<Future<Void>> futures = new ArrayList<>();
            if (mpcConcurrency > 1) {
                for (int i = 0; i < mpcConcurrency; i++) {
                    String mpcId = props.getDpi().getMpcId() + "-" + i;
                    futures.add(dpiReceiptService.handleReceipts(mpcId));
                }

            } else {
                String mpcId = props.getDpi().getMpcId();
                futures.add(dpiReceiptService.handleReceipts(mpcId));
            }
            for (Future<Void> future : futures) {
                // Waits for the async handleReceipts to complete in order to avoid the schedule to fire again and cause
                // concurrent consumption of the MPC(s).
                future.get();
            }
        }
    }
}
