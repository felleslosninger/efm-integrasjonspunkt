package no.difi.meldingsutveksling.status;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * Periodically checks  for DPI receipts.
 */
@RequiredArgsConstructor
@Slf4j
public class DpiStatusPolling {

    private final IntegrasjonspunktProperties properties;
    private final SchedulingTaskExecutor dpiReceiptExecutor;
    private final DpiReceiptService dpiReceiptService;

    @Getter(lazy = true) private final List<String> mpcIdList = fetchMpcIdList();

    private List<String> fetchMpcIdList() {
        int mpcConcurrency = properties.getDpi().getMpcConcurrency();

        if (mpcConcurrency > 1) {
            return IntStream.range(0, mpcConcurrency)
                    .mapToObj(p -> properties.getDpi().getMpcId() + "-" + p)
                    .collect(collectingAndThen(toList(), ImmutableList::copyOf));
        } else {
            return Collections.singletonList(properties.getDpi().getMpcId());
        }
    }


    @Scheduled(fixedDelayString = "${difi.move.dpi.pollingrate}")
    public void dpiReceiptsScheduledTask() throws ExecutionException, InterruptedException {
        if (properties.getFeature().isEnableReceipts() && properties.getFeature().isEnableDPI()) {
            List<Future<?>> futures = getMpcIdList()
                    .stream()
                    .map(this::submitHandleReceipts)
                    .collect(Collectors.toList());

            for (Future<?> future : futures) {
                // Waits for the async handleReceipts to complete in order to avoid the schedule to fire again and cause
                // concurrent consumption of the MPC(s).
                future.get();
            }
        }
    }

    private Future<?> submitHandleReceipts(String mpcId) {
        return dpiReceiptExecutor.submit(() -> dpiReceiptService.handleReceipts(mpcId));
    }
}
