package no.difi.meldingsutveksling.receipt;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum
public enum NextmoveReceiptStatus implements ReceiptStatus {
    @XmlEnumValue("LEST_FRA_SERVICEBUS") LEST_FRA_SERVICEBUS("LEST_FRA_SERVICEBUS"),
    @XmlEnumValue("POPPET") POPPET("POPPET");

    private String status;

    NextmoveReceiptStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return this.status;
    }
}
