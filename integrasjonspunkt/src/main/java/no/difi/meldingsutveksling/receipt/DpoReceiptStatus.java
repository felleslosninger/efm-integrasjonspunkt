package no.difi.meldingsutveksling.receipt;

import no.difi.meldingsutveksling.kvittering.xsd.Kvittering;

public enum DpoReceiptStatus implements ReceiptStatus {
    AAPNING("Åpningskvittering"),
    LEVERING("Leveringskvittering"),
    MOTTAK("Mottakskvittering"),
    VARSLINGFEILET("Varsling feilet"),
    RETURPOST("Returpost"),
    UKJENT("Ukjent");

    private String status;

    DpoReceiptStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return null;
    }

    public static DpoReceiptStatus of(Kvittering kvittering) {
        if (kvittering.getAapning() != null) {
            return AAPNING;
        }
        if (kvittering.getLevering() != null) {
            return LEVERING;
        }
        if (kvittering.getMottak() != null) {
            return MOTTAK;
        }
        if (kvittering.getVarslingfeilet() != null) {
            return VARSLINGFEILET;
        }
        if (kvittering.getReturpost() != null) {
            return RETURPOST;
        }
        return UKJENT;
    }
}
