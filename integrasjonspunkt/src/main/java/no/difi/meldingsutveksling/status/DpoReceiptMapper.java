package no.difi.meldingsutveksling.status;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;

@UtilityClass
public class DpoReceiptMapper {

    public static ReceiptStatus from(Kvittering kvittering) {
        if (kvittering.getAapning() != null) {
            return ReceiptStatus.LEVERT;
        }
        if (kvittering.getLevering() != null) {
            return ReceiptStatus.MOTTATT;
        }
        if (kvittering.getMottak() != null) {
            return ReceiptStatus.MOTTATT;
        }
        return ReceiptStatus.ANNET;
    }
}
