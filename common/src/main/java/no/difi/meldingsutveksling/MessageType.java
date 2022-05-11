package no.difi.meldingsutveksling;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
public enum MessageType {


    STATUS("status", ApiType.NEXTMOVE),
    FEIL("feil", ApiType.NEXTMOVE),

    ARKIVMELDING("arkivmelding", ApiType.NEXTMOVE),
    ARKIVMELDING_KVITTERING("arkivmelding_kvittering", ApiType.NEXTMOVE),

    AVTALT("avtalt", ApiType.NEXTMOVE),

    FIKSIO("fiksio", ApiType.NEXTMOVE),
    DIGITAL("digital", ApiType.NEXTMOVE),
    DIGITAL_DPV("digital_dpv", ApiType.NEXTMOVE),
    PRINT("print", ApiType.NEXTMOVE),

    INNSYNSKRAV("innsynskrav", ApiType.NEXTMOVE),
    PUBLISERING("publisering", ApiType.NEXTMOVE),
    EINNSYN_KVITTERING("einnsyn_kvittering", ApiType.NEXTMOVE),

    BESTEDU_KVITTERING("kvittering", ApiType.BESTEDU),
    BESTEDU_MELDING("melding", ApiType.BESTEDU);

    private static final Set<MessageType> RECEIPTS = EnumSet.of(ARKIVMELDING_KVITTERING, EINNSYN_KVITTERING, BESTEDU_KVITTERING);

    private final String type;
    private final ApiType api;

    public boolean isReceipt() {
        return RECEIPTS.contains(this);
    }

    public boolean fitsDocumentIdentifier(String documentIdentifier) {
        return documentIdentifier.endsWith("::" + type);
    }

    public static MessageType[] values(ApiType api) {
        return stream(api)
                .toArray(MessageType[]::new);
    }

    public static Optional<MessageType> valueOfType(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Arrays.stream(MessageType.values())
                .filter(p -> p.type.equalsIgnoreCase(type))
                .findAny();
    }

    public static Optional<MessageType> valueOfDocumentType(String documentType) {
        if (Strings.isNullOrEmpty(documentType)) {
            return Optional.empty();
        }
        return Arrays.stream(MessageType.values())
            .filter(d -> documentType.endsWith("::"+d.getType()))
            .findFirst();
    }

    public static Optional<MessageType> valueOf(String type, ApiType api) {
        if (type == null) {
            return Optional.empty();
        }
        return stream(api)
                .filter(p -> p.type.equalsIgnoreCase(type))
                .findAny();
    }

    public static Stream<MessageType> stream(ApiType api) {
        return Arrays.stream(MessageType.values())
                .filter(p -> p.api == api);
    }
}
