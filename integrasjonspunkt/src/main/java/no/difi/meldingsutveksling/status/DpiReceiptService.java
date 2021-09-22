package no.difi.meldingsutveksling.status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerClient;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class DpiReceiptService {

    private final MeldingsformidlerClient meldingsformidlerClient;
    private final ConversationService conversationService;
    private final AvsenderindikatorHolder avsenderindikatorHolder;

    public void handleReceipts(String mpcId) {
        Set<String> avsenderindikatorListe = avsenderindikatorHolder.getAvsenderindikatorListe();

        if (avsenderindikatorListe.isEmpty()) {
            meldingsformidlerClient.sjekkEtterKvitteringer(null, mpcId, this::handleReceipt);

        } else {
            avsenderindikatorListe.forEach(avsenderindikator ->
                    meldingsformidlerClient.sjekkEtterKvitteringer(avsenderindikator, mpcId, this::handleReceipt));
        }
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
