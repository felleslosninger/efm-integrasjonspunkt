package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.ks.receipt.DpfReceiptStatus;
import no.difi.meldingsutveksling.ks.svarut.ForsendelseStatus;

import java.util.EnumMap;

public class ForsendelseStatusMapper {
    private EnumMap<ForsendelseStatus, DpfReceiptStatus> mapping;

    public ForsendelseStatusMapper() {
        mapping = new EnumMap<>(ForsendelseStatus.class);
        mapping.put(ForsendelseStatus.LEST, DpfReceiptStatus.LEST);
        mapping.put(ForsendelseStatus.MOTTATT, DpfReceiptStatus.MOTTATT);
        mapping.put(ForsendelseStatus.AVVIST, DpfReceiptStatus.AVVIST);
        mapping.put(ForsendelseStatus.AKSEPTERT, DpfReceiptStatus.AKSEPTERT);
        mapping.put(ForsendelseStatus.IKKE_LEVERT, DpfReceiptStatus.IKKE_LEVERT);
        mapping.put(ForsendelseStatus.MANUELT_HANDTERT, DpfReceiptStatus.MANULT_HANDTERT);
        mapping.put(ForsendelseStatus.LEVERT_SDP, DpfReceiptStatus.LEVERT_SDP);
        mapping.put(ForsendelseStatus.PRINTET, DpfReceiptStatus.PRINTET);
        mapping.put(ForsendelseStatus.SENDT_DIGITALT, DpfReceiptStatus.SENDT_DIGITALT);
        mapping.put(ForsendelseStatus.SENDT_PRINT, DpfReceiptStatus.SENDT_PRINT);
        mapping.put(ForsendelseStatus.SENDT_SDP, DpfReceiptStatus.SENDT_SDP);
        mapping.put(ForsendelseStatus.VARSLET, DpfReceiptStatus.VARSLET);
    }

    public DpfReceiptStatus mapFrom(ForsendelseStatus forsendelseStatus) {
        return mapping.get(forsendelseStatus);
    }
}
