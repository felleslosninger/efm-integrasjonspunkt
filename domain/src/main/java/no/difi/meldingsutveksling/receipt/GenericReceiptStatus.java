package no.difi.meldingsutveksling.receipt;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum(String.class)
public enum GenericReceiptStatus implements ReceiptStatus {
    @XmlEnumValue("OPPRETTET")OPPRETTET("OPPRETTET"),
    @XmlEnumValue("SENDT")SENDT("Sendt"),
    @XmlEnumValue("LEVERT")LEVERT("Levert"),
    @XmlEnumValue("LEST")LEST("Lest"),
    @XmlEnumValue("FEIL")FEIL("Feil"),
    @XmlEnumValue("ANNET")ANNET("Annet");

    private final String status;

    GenericReceiptStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return status;
    }

}
