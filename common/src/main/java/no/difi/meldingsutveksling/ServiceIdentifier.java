package no.difi.meldingsutveksling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import java.util.Arrays;

@XmlType
@XmlEnum(String.class)
@RequiredArgsConstructor
@Getter
public enum ServiceIdentifier {

    @XmlEnumValue("DPO") DPO("DPO", "urn:no:difi:meldingsutveksling:2.0"),
    @XmlEnumValue("DPV") DPV("DPV", null),
    @XmlEnumValue("DPI") DPI("DPI", null),
    @XmlEnumValue("DPF") DPF("DPF", null),
    @XmlEnumValue("DPE_INNSYN") DPE_INNSYN("DPE_innsyn", null),
    @XmlEnumValue("DPE_DATA") DPE_DATA("DPE_data", null),
    @XmlEnumValue("DPE_RECEIPT") DPE_RECEIPT("DPE_RECEIPT", null),
    UNKNOWN("ukjent", null);

    private final String fullname;
    private final String standard;

    public static ServiceIdentifier safeValueOf(String s) {
        if (s == null) {
            return UNKNOWN;
        }

        return Arrays.stream(ServiceIdentifier.values())
                .filter(p -> p.name().equals(s))
                .findAny()
                .orElse(UNKNOWN);
    }
}
