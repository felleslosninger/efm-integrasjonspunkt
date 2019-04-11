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
    @XmlEnumValue("DPE_INNSYN") DPE_INNSYN("DPE_innsyn"),
    @XmlEnumValue("DPE_DATA") DPE_DATA("DPE_data"),
    @XmlEnumValue("DPE_RECEIPT") DPE_RECEIPT("DPE_RECEIPT"),
    UNKNOWN("UNKNOWN");

    private final String fullname;
}
