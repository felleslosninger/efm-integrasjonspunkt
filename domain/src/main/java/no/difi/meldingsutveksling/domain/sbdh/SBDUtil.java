package no.difi.meldingsutveksling.domain.sbdh;

import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.MessageInfo;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;

import java.util.Optional;
import java.util.UUID;

public class SBDUtil {

    private SBDUtil() {
        // UtilityClass
    }

    public static String getSender(StandardBusinessDocument sbd) {
        return getOptionalSender(sbd)
                .orElse(null);
    }

    public static Optional<String> getOptionalSender(StandardBusinessDocument sbd) {
        return StandardBusinessDocumentUtils.getFirstSender(sbd)
                .flatMap(p -> Optional.ofNullable(p.getIdentifier()))
                .flatMap(p -> Optional.ofNullable(p.getValue()));
    }

    public static Optional<String> getOptionalSender(StandardBusinessDocumentHeader sbdh) {
        return sbdh.getFirstSender()
                .flatMap(p -> Optional.ofNullable(p.getIdentifier()))
                .flatMap(p -> Optional.ofNullable(p.getValue()));
    }

    public static String getReceiver(StandardBusinessDocument sbd) {
        return getOptionalReceiver(sbd)
                .orElse(null);
    }

    public static Optional<String> getOptionalReceiver(StandardBusinessDocument sbd) {
        return StandardBusinessDocumentUtils.getFirstReceiver(sbd)
                .flatMap(p -> Optional.ofNullable(p.getIdentifier()))
                .flatMap(p -> Optional.ofNullable(p.getValue()));
    }

    public static String getDocumentType(StandardBusinessDocument sbd) {
        return StandardBusinessDocumentUtils.getDocumentType(sbd)
                .orElseThrow(MeldingsUtvekslingRuntimeException::new);
    }

    public static String getConversationId(StandardBusinessDocument sbd) {
        return getOptionalConversationId(sbd)
                .orElseThrow(MeldingsUtvekslingRuntimeException::new);
    }

    public static Optional<String> getOptionalConversationId(StandardBusinessDocument sbd) {
        return StandardBusinessDocumentUtils.getScope(sbd, ScopeType.CONVERSATION_ID)
                .map(Scope::getInstanceIdentifier);
    }

    public static Optional<Scope> getOptionalMessageChannel(StandardBusinessDocument sbd) {
        return StandardBusinessDocumentUtils.getScope(sbd, ScopeType.MESSAGE_CHANNEL);
    }

    public static String getProcess(StandardBusinessDocument sbd) {
        return StandardBusinessDocumentUtils.getScope(sbd, ScopeType.CONVERSATION_ID)
                .flatMap(p -> Optional.of(p.getIdentifier()))
                .orElseThrow(() -> new NextMoveRuntimeException("Couldn't retrieve process from SBD"));
    }

    public static Optional<String> getOptionalReceiverRef(StandardBusinessDocument sbd) {
        return StandardBusinessDocumentUtils.getScope(sbd, ScopeType.RECEIVER_REF)
                .flatMap(p -> Optional.of(p.getInstanceIdentifier()));
    }

    public static Optional<String> getOptionalSenderRef(StandardBusinessDocument sbd) {
        return StandardBusinessDocumentUtils.getScope(sbd, ScopeType.SENDER_REF)
                .flatMap(p -> Optional.of(p.getInstanceIdentifier()));
    }

    public static String getJournalPostId(StandardBusinessDocument sbd) {
        return StandardBusinessDocumentUtils.getScope(sbd, ScopeType.JOURNALPOST_ID)
                .map(Scope::getInstanceIdentifier)
                .orElse("");
    }

    public static String getMessageId(StandardBusinessDocument sbd) {
        if (sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().getInstanceIdentifier() == null) {
            sbd.getStandardBusinessDocumentHeader().getDocumentIdentification().setInstanceIdentifier(UUID.randomUUID().toString());
        }
        return StandardBusinessDocumentUtils.getMessageId(sbd)
                .orElseThrow(() -> new NextMoveRuntimeException("Couldn't retrieve messageId from SBD"));
    }

    public static boolean isNextMove(StandardBusinessDocument sbd) {
        return getOptionalMessageType(sbd)
                .map(MessageType::getApi)
                .filter(p -> p == ApiType.NEXTMOVE)
                .isPresent();
    }

    public static boolean isReceipt(StandardBusinessDocument sbd) {
        return getOptionalMessageType(sbd)
                .filter(MessageType::isReceipt)
                .isPresent();
    }

    public static boolean isStatus(StandardBusinessDocument sbd) {
        return getOptionalMessageType(sbd)
                .filter(dt -> dt == MessageType.STATUS)
                .isPresent();
    }

    public static Optional<MessageType> getOptionalMessageType(StandardBusinessDocument sbd) {
        return StandardBusinessDocumentUtils.getType(sbd)
                .flatMap(MessageType::valueOfType);
    }

    public static MessageType getMessageType(StandardBusinessDocument sbd) {
        return StandardBusinessDocumentUtils.getType(sbd)
                .flatMap(MessageType::valueOfType)
                .orElseThrow(() -> new NextMoveRuntimeException("Couldn't retrieve messageType from SBD"));
    }

    public static boolean isType(StandardBusinessDocument sbd, MessageType messageType) {
        return getOptionalMessageType(sbd)
                .filter(dt -> dt == messageType)
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
                getMessageType(sbd).getType(),
                getReceiver(sbd),
                getSender(sbd),
                getJournalPostId(sbd),
                getConversationId(sbd),
                getMessageId(sbd));
    }
}
