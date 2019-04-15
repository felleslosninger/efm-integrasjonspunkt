package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;

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
