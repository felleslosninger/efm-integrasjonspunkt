package no.difi.meldingsutveksling.domain.sbdh;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.Set;

@Slf4j
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

    public static boolean isExpired(StandardBusinessDocumentHeader header) {
        ZonedDateTime zonedDateTime = header.getExpectedResponseTimeDate();
        ZonedDateTime now = ZonedDateTime.now();
        if (zonedDateTime.isAfter(now) || now.equals(zonedDateTime)) {
            return true;
        }
        else {
            log.error("ExpectedResponseDateTime is expired. Message will not be handled further. Please resend...");
            return false;}
    }
}
