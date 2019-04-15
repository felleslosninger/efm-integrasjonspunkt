package no.difi.meldingsutveksling.receipt;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum(String.class)
public enum GenericReceiptStatus implements ReceiptStatus {
    @XmlEnumValue("OPPRETTET")OPPRETTET,
    @XmlEnumValue("SENDT")SENDT,
    @XmlEnumValue("LEVERT")LEVERT,
    @XmlEnumValue("LEST")LEST,
    @XmlEnumValue("FEIL")FEIL,
    @XmlEnumValue("ANNET")ANNET,
    @XmlEnumValue("INNKOMMENDE_MOTTATT")INNKOMMENDE_MOTTATT,
    @XmlEnumValue("INNKOMMENDE_LEVERT")INNKOMMENDE_LEVERT;

    @Override
    public String getStatus() {
        return this.name();
    }

}
