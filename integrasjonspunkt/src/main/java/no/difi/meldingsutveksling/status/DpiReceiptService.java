package no.difi.meldingsutveksling.status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class DpiReceiptService {

    private final MeldingsformidlerClient meldingsformidlerClient;
    private final ConversationService conversationService;
    private final SchedulingTaskExecutor dpiReceiptExecutor;

    public void handleReceipts() throws ExecutionException, InterruptedException {
        List<Future<?>> futures = meldingsformidlerClient.getPartitionIds()
                .stream()
                .map(this::submitHandleReceipts)
                .collect(Collectors.toList());

        for (Future<?> future : futures) {
            // Waits for the async handleReceipts to complete in order to avoid the schedule to fire again and cause
            // concurrent consumption of the MPC(s).
            future.get();
        }
    }

    private Future<?> submitHandleReceipts(String partitionId) {
        return dpiReceiptExecutor.submit(() -> handleReceipts(partitionId));
    }

    private void handleReceipts(String mpcId) {
        meldingsformidlerClient.sjekkEtterKvitteringer(mpcId, this::handleReceipt);
    }

    @Transactional
    public void handleReceipt(ExternalReceipt externalReceipt) {
        final String conversationId = externalReceipt.getConversationId();
        Conversation conversation = conversationService.findConversation(conversationId, ConversationDirection.OUTGOING)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown conversationID = %s", conversationId)));

        conversationService.registerStatus(conversation, externalReceipt.toMessageStatus());

        log.debug(externalReceipt.logMarkers(), "Updated receipt (DPI)");
        externalReceipt.confirmReceipt();
        log.debug(externalReceipt.logMarkers(), "Confirmed receipt (DPI)");
    }
}
