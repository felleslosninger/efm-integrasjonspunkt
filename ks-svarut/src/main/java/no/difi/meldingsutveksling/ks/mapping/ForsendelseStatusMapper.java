package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.ks.ForsendelseStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;

import java.util.EnumMap;

public class ForsendelseStatusMapper {
    private EnumMap<ForsendelseStatus, ReceiptStatus> mapping;

    public ForsendelseStatusMapper() {
        mapping = new EnumMap<>(ForsendelseStatus.class);
        mapping.put(ForsendelseStatus.LEST, ReceiptStatus.READ);
        mapping.put(ForsendelseStatus.MOTTATT, ReceiptStatus.DELIVERED);
        mapping.put(ForsendelseStatus.AVVIST, ReceiptStatus.FAIL);
        mapping.put(ForsendelseStatus.AKSEPTERT, ReceiptStatus.SENT);
        mapping.put(ForsendelseStatus.IKKE_LEVERT, ReceiptStatus.OTHER);
        mapping.put(ForsendelseStatus.MANUELT_HANDTERT, ReceiptStatus.DELIVERED);
        mapping.put(ForsendelseStatus.LEVERT_SDP, ReceiptStatus.DELIVERED);
        mapping.put(ForsendelseStatus.PRINTET, ReceiptStatus.DELIVERED);
        mapping.put(ForsendelseStatus.SENDT_DIGITALT, ReceiptStatus.DELIVERED);
        mapping.put(ForsendelseStatus.SENDT_PRINT, ReceiptStatus.DELIVERED);
        mapping.put(ForsendelseStatus.SENDT_SDP, ReceiptStatus.DELIVERED);
        mapping.put(ForsendelseStatus.VARSLET, ReceiptStatus.DELIVERED);
    }

    public ReceiptStatus mapFrom(ForsendelseStatus forsendelseStatus) {
        return mapping.get(forsendelseStatus);
    }
}
