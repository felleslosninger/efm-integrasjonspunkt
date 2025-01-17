package no.difi.meldingsutveksling.dpi.client.domain.messagetypes;

import lombok.Getter;
import no.difi.meldingsutveksling.jackson.StandardBusinessDocumentType;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
public enum DpiMessageType implements StandardBusinessDocumentType {

    DIGITAL("digital", Digital.class, Direction.OUTGOING),
    UTSKRIFT("utskrift", Utskrift.class, Direction.OUTGOING),
    FLYTTET("flyttet", Flyttet.class, Direction.OUTGOING),
    FEIL("feil", Feil.class, Direction.INCOMING),
    LEVERINGSKVITTERING("leveringskvittering", Leveringskvittering.class, Direction.INCOMING),
    AAPNINGSKVITTERING("aapningskvittering", Aapningskvittering.class, Direction.INCOMING),
    VARSLINGFEILETKVITTERING("varslingfeiletkvittering", Varslingfeiletkvittering.class, Direction.INCOMING),
    MOTTAKSKVITTERING("mottakskvittering", Mottakskvittering.class, Direction.INCOMING),
    RETURPOSTKVITTERING("returpostkvittering", Returpostkvittering.class, Direction.INCOMING);

    private final String type;
    private final Class<? extends BusinessMessage> clazz;
    private final Direction direction;
    private final String standard;
    private final String process;

    DpiMessageType(String type, Class<? extends BusinessMessage> clazz, Direction direction) {
        this.type = type;
        this.clazz = clazz;
        this.direction = direction;
        this.standard = "urn:fdc:digdir.no:2020:innbyggerpost:xsd::innbyggerpost##urn:fdc:digdir.no:2020:innbyggerpost:schema:%s::1.0".formatted(type);
        this.process = direction == Direction.OUTGOING ? "urn:fdc:digdir.no:2020:profile:egovernment:innbyggerpost:%s:ver1.0".formatted(type) : null;
    }

    public static DpiMessageType fromType(String type) {
        return Arrays.stream(DpiMessageType.values()).filter(p -> p.getType().equalsIgnoreCase(type))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown MessageType = %s. Expecting one of %s",
                        type,
                        Arrays.stream(values()).map(DpiMessageType::getType).collect(Collectors.joining(",")))));
    }

    public static DpiMessageType fromClass(BusinessMessage businessMessage, Direction direction) {
        return Arrays.stream(DpiMessageType.values())
                .filter(p -> p.getClazz().isInstance(businessMessage))
                .filter(p -> p.getDirection() == direction)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown BusinessMessage = %s. Expecting one of %s",
                        businessMessage.getClass().getSimpleName(),
                        Arrays.stream(values())
                                .filter(p -> p.getDirection() == direction)
                                .map(DpiMessageType::getClazz).map(Class::getSimpleName).collect(Collectors.joining(",")))));
    }

    @Override
    public String getFieldName() {
        return type;
    }

    @Override
    public Class<?> getValueType() {
        return clazz;
    }
}
