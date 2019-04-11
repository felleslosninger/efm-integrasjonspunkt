package no.difi.meldingsutveksling.domain.sbdh;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.DocumentType;

@UtilityClass
public class SBDUtil {

    public static boolean isNextMove(StandardBusinessDocument sbd) {
        return DocumentType.valueOfType(sbd.getMessageType())
                .map(DocumentType::getApi)
                .map(p -> p == ApiType.NEXTMOVE)
                .orElse(false);
    }

    public static boolean isReceipt(StandardBusinessDocument sbd) {
        return DocumentType.valueOfType(sbd.getMessageType())
                .map(DocumentType::isReceipt)
                .orElse(false);
    }
}
