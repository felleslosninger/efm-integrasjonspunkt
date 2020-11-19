package no.difi.meldingsutveksling.domain.sbdh;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.DocumentType;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

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

    private boolean isExpired(OffsetDateTime expectedResponseDateTime) {
        OffsetDateTime currentDateTime = OffsetDateTime.now(clock);
        return currentDateTime.isAfter(expectedResponseDateTime);
    }

    public boolean isArkivmelding(StandardBusinessDocument sbd) {
        return (isType(sbd, DocumentType.ARKIVMELDING)) || (isType(sbd, DocumentType.ARKIVMELDING_KVITTERING));
    }

    public boolean isEinnsyn(StandardBusinessDocument sbd) {
        return isType(sbd, DocumentType.INNSYNSKRAV) || isType(sbd, DocumentType.PUBLISERING) || isType(sbd, DocumentType.EINNSYN_KVITTERING);
    }

    public boolean hasVersion(StandardBusinessDocument sbd, String version) {
        return sbd.getProcess().contains(version);
    }

    public boolean isFileRequired(StandardBusinessDocument sbd) {
        return !isStatus(sbd) &&
                !isReceipt(sbd) &&
                !isType(sbd, DocumentType.AVTALT);
    }
}
