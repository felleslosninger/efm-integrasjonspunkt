package no.difi.meldingsutveksling.domain.sbdh;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.MessageType;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class SBDUtil {

    private final Clock clock;

    public boolean isReceipt(StandardBusinessDocument sbd) {
        return MessageType.valueOfType(sbd.getMessageType())
                .map(MessageType::isReceipt)
                .orElse(false);
    }

    public boolean isStatus(StandardBusinessDocument sbd) {
        return MessageType.valueOfType(sbd.getMessageType())
                .map(dt -> dt == MessageType.STATUS)
                .orElse(false);
    }

    public boolean isType(StandardBusinessDocument sbd, MessageType messageType) {
        return MessageType.valueOfType(sbd.getMessageType())
                .map(dt -> dt == messageType)
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
        return (isType(sbd, MessageType.ARKIVMELDING)) || (isType(sbd, MessageType.ARKIVMELDING_KVITTERING));
    }

    public boolean isEinnsyn(StandardBusinessDocument sbd) {
        return isType(sbd, MessageType.INNSYNSKRAV) || isType(sbd, MessageType.PUBLISERING) || isType(sbd, MessageType.EINNSYN_KVITTERING);
    }

    public boolean isFileRequired(StandardBusinessDocument sbd) {
        return !isStatus(sbd) &&
                !isReceipt(sbd) &&
                !isType(sbd, MessageType.AVTALT);
    }
}
