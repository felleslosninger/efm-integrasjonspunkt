package no.difi.meldingsutveksling.domain.sbdh;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.DocumentType;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZonedDateTime;

@Component
@RequiredArgsConstructor
public class SBDUtil {

    private final Clock clock;

    public boolean isNextMove(StandardBusinessDocument sbd) {
        return DocumentType.valueOfType(sbd.getMessageType())
                .map(DocumentType::getApi)
                .map(p -> p == ApiType.NEXTMOVE)
                .orElse(false);
    }

    public boolean isReceipt(StandardBusinessDocument sbd) {
        return DocumentType.valueOfType(sbd.getMessageType())
                .map(DocumentType::isReceipt)
                .orElse(false);
    }

    public boolean isStatus(StandardBusinessDocument sbd) {
        return DocumentType.valueOfType(sbd.getMessageType())
                .map(dt -> dt == DocumentType.STATUS)
                .orElse(false);
    }

    public boolean isType(StandardBusinessDocument sbd, DocumentType documentType) {
        return DocumentType.valueOfType(sbd.getMessageType())
                .map(dt -> dt == documentType)
                .orElse(false);
    }

    public boolean isExpired(StandardBusinessDocument sbd) {
        return sbd.getExpectedResponseDateTime()
                .map(this::isExpired)
                .orElse(false);
    }

    public boolean isExpired(ZonedDateTime expectedResponseDateTime) {
        ZonedDateTime currentDateTime = ZonedDateTime.now(clock);
        return currentDateTime.isAfter(expectedResponseDateTime);
    }
}
