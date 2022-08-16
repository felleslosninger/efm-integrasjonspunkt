package no.difi.meldingsutveksling.domain.sbdh;

import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.MessageInfo;

import java.util.Optional;

public class SBDUtil {

    private SBDUtil() {
        // UtilityClass
    }

    public static Optional<Iso6523> getSenderPartIdentifier(StandardBusinessDocument sbd) {
        return Optional.ofNullable(sbd)
                .map(StandardBusinessDocument::getSenderIdentifier)
                .flatMap(p -> p.as(Iso6523.class))
                .filter(Iso6523::hasOrganizationPartIdentifier)
                .map(o -> Iso6523.of(o.getIcd(), o.getOrganizationPartIdentifier()));
    }

    public static Optional<Iso6523> getReceiverPartIdentifier(StandardBusinessDocument sbd) {
        return Optional.ofNullable(sbd)
                .map(StandardBusinessDocument::getReceiverIdentifier)
                .flatMap(p -> p.as(Iso6523.class))
                .filter(Iso6523::hasOrganizationPartIdentifier)
                .map(o -> Iso6523.of(o.getIcd(), o.getOrganizationPartIdentifier()));
    }

    public static Optional<Iso6523> getSenderPartIdentifier(StandardBusinessDocumentHeader sbdh) {
        return Optional.ofNullable(sbdh)
                .map(StandardBusinessDocumentHeader::getSenderIdentifier)
                .flatMap(p -> p.as(Iso6523.class))
                .filter(Iso6523::hasOrganizationPartIdentifier)
                .map(o -> Iso6523.of(o.getIcd(), o.getOrganizationPartIdentifier()));
    }

    public static Optional<Scope> getOptionalMessageChannel(StandardBusinessDocument sbd) {
        return sbd.getScope(ScopeType.MESSAGE_CHANNEL);
    }

    public static Optional<String> getOptionalReceiverRef(StandardBusinessDocument sbd) {
        return sbd.getScope(ScopeType.RECEIVER_REF)
                .flatMap(p -> Optional.of(p.getInstanceIdentifier()));
    }

    public static Optional<String> getOptionalSenderRef(StandardBusinessDocument sbd) {
        return sbd.getScope(ScopeType.SENDER_REF)
                .flatMap(p -> Optional.of(p.getInstanceIdentifier()));
    }

    public static String getJournalPostId(StandardBusinessDocument sbd) {
        return sbd.getScope(ScopeType.JOURNALPOST_ID)
                .map(Scope::getInstanceIdentifier)
                .orElse("");
    }

    public static boolean isNextMove(StandardBusinessDocument sbd) {
        return MessageType.valueOfType(sbd.getType())
                .map(MessageType::getApi)
                .filter(t -> t == ApiType.NEXTMOVE)
                .isPresent();
    }

    public static boolean isReceipt(StandardBusinessDocument sbd) {
        return MessageType.valueOfType(sbd.getType())
                .filter(MessageType::isReceipt)
                .isPresent();
    }

    public static boolean isStatus(StandardBusinessDocument sbd) {
        return MessageType.valueOfType(sbd.getType())
                .filter(dt -> dt == MessageType.STATUS)
                .isPresent();
    }

    public static boolean isType(StandardBusinessDocument sbd, MessageType messageType) {
        return MessageType.valueOfType(sbd.getType())
                .filter(mt -> mt == messageType)
                .isPresent();
    }

    public static boolean isArkivmelding(StandardBusinessDocument sbd) {
        return (isType(sbd, MessageType.ARKIVMELDING)) || (isType(sbd, MessageType.ARKIVMELDING_KVITTERING));
    }

    public static boolean isAvtalt(StandardBusinessDocument sbd) {
        return (isType(sbd, MessageType.AVTALT));
    }

    public static boolean isEinnsyn(StandardBusinessDocument sbd) {
        return isType(sbd, MessageType.INNSYNSKRAV) || isType(sbd, MessageType.PUBLISERING) || isType(sbd, MessageType.EINNSYN_KVITTERING);
    }

    public static boolean isFileRequired(StandardBusinessDocument sbd) {
        return !isStatus(sbd) &&
                !isReceipt(sbd) &&
                !isType(sbd, MessageType.AVTALT);
    }

    public static MessageInfo getMessageInfo(StandardBusinessDocument sbd) {
        return new MessageInfo(
                sbd.getType(),
                sbd.getReceiverIdentifier(),
                sbd.getSenderIdentifier(),
                getJournalPostId(sbd),
                sbd.getConversationId(),
                sbd.getMessageId());
    }
}
