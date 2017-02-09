package no.difi.meldingsutveksling;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum(String.class)
public enum ServiceIdentifier {
    @XmlEnumValue("EDU")    EDU("EDU"),
    @XmlEnumValue("DPV")    DPV("POST_VIRKSOMHET"),
    @XmlEnumValue("DPI")    DPI("DPI"),
    @XmlEnumValue("FIKS")   FIKS("FIKS");

    private final String fullname;

    ServiceIdentifier(String fullname) {
        this.fullname = fullname;
    }

    public String fullname() {
        return fullname;
    }
}
