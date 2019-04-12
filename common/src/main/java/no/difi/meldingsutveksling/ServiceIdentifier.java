package no.difi.meldingsutveksling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum(String.class)
@RequiredArgsConstructor
@Getter
public enum ServiceIdentifier {

    @XmlEnumValue("DPO") DPO("DPO"),
    @XmlEnumValue("DPV") DPV("DPV"),
    @XmlEnumValue("DPI_DIGITAL") DPI_DIGITAL("DPI_DIGITAL"),
    @XmlEnumValue("DPI_PRINT") DPI_PRINT("DPI_PRINT"),
    @XmlEnumValue("DPF") DPF("DPF"),
    @XmlEnumValue("DPE") DPE("DPE"),
    UNKNOWN("UNKNOWN");

    private final String fullname;
}
