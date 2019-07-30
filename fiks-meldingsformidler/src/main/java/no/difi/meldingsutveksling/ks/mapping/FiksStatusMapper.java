package no.difi.meldingsutveksling.ks.mapping;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ks.svarut.ForsendelseStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FiksStatusMapper {

    private final MessageStatusFactory messageStatusFactory;

    public MessageStatus mapFrom(ForsendelseStatus forsendelseStatus) {
        switch (forsendelseStatus) {
            case LEST:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEST);
            case MOTTATT:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.MOTTATT);
            case AVVIST:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL, "Avvist");
            case AKSEPTERT:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEVERT, "Akseptert");
            case IKKE_LEVERT:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL, "Ikke levert");
            case MANUELT_HANDTERT:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEST, "Manuelt håndtert");
            case LEVERT_SDP:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.ANNET, "Levert SDP");
            case PRINTET:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.ANNET, "Printet");
            case SENDT_DIGITALT:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEST, "Sendt digitalt");
            case SENDT_PRINT:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEST, "Sendt print");
            case SENDT_SDP:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEST, "Sendt SDP");
            case VARSLET:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.ANNET, "Varslet");
            case KLAR_FOR_MOTTAK:
                return messageStatusFactory.getMessageStatus(ReceiptStatus.LEVERT, "Klar for mottak");
            default:
                return null;
        }
    }

    public MessageStatus noForsendelseId() {
        return messageStatusFactory.getMessageStatus(ReceiptStatus.FEIL, "forsendelseId finnes ikke i SvarUt.");
    }
}
