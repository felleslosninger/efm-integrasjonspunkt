package no.difi.meldingsutveksling;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter
public enum DocumentType {

    STATUS("status", ApiType.NEXTMOVE),

    ARKIVMELDING_KVITTERING("arkivmelding_kvittering", ApiType.NEXTMOVE),
    ARKIVMELDING("arkivmelding", ApiType.NEXTMOVE, ARKIVMELDING_KVITTERING),

    DIGITAL("digital", ApiType.NEXTMOVE),
    PRINT("print", ApiType.NEXTMOVE),

    EINNSYN_KVITTERING("einnsyn_kvittering", ApiType.NEXTMOVE),
    INNSYNSKRAV("innsynskrav", ApiType.NEXTMOVE, EINNSYN_KVITTERING),
    PUBLISERING("publisering", ApiType.NEXTMOVE, EINNSYN_KVITTERING),

    BESTEDU_KVITTERING("kvittering", ApiType.BESTEDU),
    BESTEDU_MELDING("melding", ApiType.BESTEDU, BESTEDU_KVITTERING);

    private static final Set<DocumentType> RECEIPTS = EnumSet.of(ARKIVMELDING_KVITTERING, EINNSYN_KVITTERING, BESTEDU_KVITTERING);

    private final String type;
    private final ApiType api;
    private DocumentType receipt;

    public boolean isReceipt() {
        return RECEIPTS.contains(this);
    }

    public boolean fitsStandard(String standard) {
        return standard.endsWith("::" + type);
    }

    public static DocumentType[] values(ApiType api) {
        return stream(api)
                .toArray(DocumentType[]::new);
    }

    public static Optional<DocumentType> valueOfType(String type) {
        if (type == null) {
            return Optional.empty();
        }
        return Arrays.stream(DocumentType.values())
                .filter(p -> p.type.equalsIgnoreCase(type))
                .findAny();
    }

    public static Optional<DocumentType> valueOf(String type, ApiType api) {
        if (type == null) {
            return Optional.empty();
        }
        return stream(api)
                .filter(p -> p.type.equalsIgnoreCase(type))
                .findAny();
    }

    public static Stream<DocumentType> stream(ApiType api) {
        return Arrays.stream(DocumentType.values())
                .filter(p -> p.api == api);
    }

}
