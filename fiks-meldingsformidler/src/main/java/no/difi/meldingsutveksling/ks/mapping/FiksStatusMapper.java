package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.ks.svarut.ForsendelseStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;

import java.util.EnumMap;

public class FiksStatusMapper {
    private static EnumMap<ForsendelseStatus, MessageStatus> mapping;

    static {
        mapping = new EnumMap<>(ForsendelseStatus.class);
        mapping.put(ForsendelseStatus.LEST, MessageStatus.of(ReceiptStatus.LEST));
        mapping.put(ForsendelseStatus.MOTTATT, MessageStatus.of(ReceiptStatus.MOTTATT));
        mapping.put(ForsendelseStatus.AVVIST, MessageStatus.of(ReceiptStatus.FEIL, "Avvist"));
        mapping.put(ForsendelseStatus.AKSEPTERT, MessageStatus.of(ReceiptStatus.LEVERT, "Akseptert"));
        mapping.put(ForsendelseStatus.IKKE_LEVERT, MessageStatus.of(ReceiptStatus.FEIL, "Ikke levert"));
        mapping.put(ForsendelseStatus.MANUELT_HANDTERT, MessageStatus.of(ReceiptStatus.LEST, "Manuelt håndtert"));
        mapping.put(ForsendelseStatus.LEVERT_SDP, MessageStatus.of(ReceiptStatus.ANNET, "Levert SDP"));
        mapping.put(ForsendelseStatus.PRINTET, MessageStatus.of(ReceiptStatus.ANNET, "Printet"));
        mapping.put(ForsendelseStatus.SENDT_DIGITALT, MessageStatus.of(ReceiptStatus.LEST, "Sendt digitalt"));
        mapping.put(ForsendelseStatus.SENDT_PRINT, MessageStatus.of(ReceiptStatus.LEST, "Sendt print"));
        mapping.put(ForsendelseStatus.SENDT_SDP, MessageStatus.of(ReceiptStatus.LEST, "Sendt SDP"));
        mapping.put(ForsendelseStatus.VARSLET, MessageStatus.of(ReceiptStatus.ANNET, "Varslet"));
        mapping.put(ForsendelseStatus.KLAR_FOR_MOTTAK, MessageStatus.of(ReceiptStatus.LEVERT, "Klar for mottak"));
    }

    private FiksStatusMapper() {
    }

    public static MessageStatus mapFrom(ForsendelseStatus forsendelseStatus) {
        return mapping.get(forsendelseStatus);
    }
}
