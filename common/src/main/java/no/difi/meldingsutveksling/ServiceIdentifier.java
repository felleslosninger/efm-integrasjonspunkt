package no.difi.meldingsutveksling;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum(String.class)
@RequiredArgsConstructor
@Getter
public enum ServiceIdentifier {

    @XmlEnumValue("DPO") DPO("DPO"),
    @XmlEnumValue("DPV") DPV("DPV"),
    @XmlEnumValue("DPI") DPI("DPI"),
    @XmlEnumValue("DPF") DPF("DPF"),
    @XmlEnumValue("DPFIO") DPFIO("DPFIO"),
    @XmlEnumValue("DPE") DPE("DPE"),
    @XmlEnumValue("DPH") DPH("DPH"),
    UNKNOWN("UNKNOWN");

    private final String fullname;
}
