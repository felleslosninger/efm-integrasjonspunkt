package no.difi.meldingsutveksling;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
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
    @XmlEnumValue("DPI") DPI("DPI"),
    @XmlEnumValue("DPF") DPF("DPF"),
    @XmlEnumValue("DPFIO") DPFIO("DPFIO"),
    @XmlEnumValue("DPE") DPE("DPE"),
    @JsonEnumDefaultValue UNKNOWN("UNKNOWN");

    private final String fullname;
}
