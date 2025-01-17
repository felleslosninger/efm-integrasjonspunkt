package no.difi.meldingsutveksling.receipt;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum(String.class)
public enum ReceiptStatus {
    @XmlEnumValue("OPPRETTET") OPPRETTET,
    @XmlEnumValue("SENDT") SENDT,
    @XmlEnumValue("MOTTATT") MOTTATT,
    @XmlEnumValue("LEVERT") LEVERT,
    @XmlEnumValue("LEST") LEST,
    @XmlEnumValue("FEIL") FEIL,
    @XmlEnumValue("ANNET") ANNET,
    @XmlEnumValue("INNKOMMENDE_MOTTATT") INNKOMMENDE_MOTTATT,
    @XmlEnumValue("INNKOMMENDE_LEVERT") INNKOMMENDE_LEVERT,
    @XmlEnumValue("LEVETID_UTLOPT") LEVETID_UTLOPT
}
