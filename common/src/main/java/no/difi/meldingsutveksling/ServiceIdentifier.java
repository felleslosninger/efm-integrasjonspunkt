package no.difi.meldingsutveksling;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum(String.class)
public enum ServiceIdentifier {

    @XmlEnumValue("DPO")         DPO("DPO"),
    @XmlEnumValue("DPV")         DPV("DPV"),
    @XmlEnumValue("DPI")         DPI("DPI"),
    @XmlEnumValue("DPF")         DPF("DPF"),
    @XmlEnumValue("DPE_INNSYN")  DPE_INNSYN("DPE_innsyn"),
    @XmlEnumValue("DPE_DATA")    DPE_DATA("DPE_data"),
    @XmlEnumValue("DPE_RECEIPT") DPE_RECEIPT("DPE_RECEIPT"),
    UNKNOWN("ukjent");

    private final String fullname;

    ServiceIdentifier(String fullname) {
        this.fullname = fullname;
    }

    public String fullname() {
        return fullname;
    }

}
