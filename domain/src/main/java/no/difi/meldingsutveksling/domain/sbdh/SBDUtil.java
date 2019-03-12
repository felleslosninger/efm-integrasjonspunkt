package no.difi.meldingsutveksling.domain.sbdh;

import com.google.common.collect.Sets;

import java.util.Set;

public class SBDUtil {

    private static Set<String> NEXTMOVE_STANDARDS = Sets.newHashSet(
            StandardBusinessDocumentHeader.STANDARD_BESTEDU,
            StandardBusinessDocumentHeader.STANDARD_NEXTMOVE,
            StandardBusinessDocumentHeader.STANDARD_DPO,
            StandardBusinessDocumentHeader.STANDARD_DPV,
            StandardBusinessDocumentHeader.STANDARD_DPI,
            StandardBusinessDocumentHeader.STANDARD_DPE_DATA,
            StandardBusinessDocumentHeader.STANDARD_DPE_INNSYN
    );

    public static boolean isNextMove(StandardBusinessDocument sbd) {
        return sbd.getConversationScope()
                .map(Scope::getIdentifier)
                .filter(s -> NEXTMOVE_STANDARDS.contains(s))
                .isPresent();
    }

    public static boolean isReceipt(StandardBusinessDocument sbd) {
        // TODO add nextmove receipt doc types
        return sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getType().equalsIgnoreCase(StandardBusinessDocumentHeader.KVITTERING_TYPE);
    }
}
