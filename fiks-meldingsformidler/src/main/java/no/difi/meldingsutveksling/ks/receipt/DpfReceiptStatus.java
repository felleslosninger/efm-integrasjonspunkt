package no.difi.meldingsutveksling.ks.receipt;

import no.difi.meldingsutveksling.receipt.ReceiptStatus;

public enum DpfReceiptStatus implements ReceiptStatus {
    LEST("Lest"),
    MOTTATT("Mottatt"),
    AVVIST("Avvist"),
    AKSEPTERT("Akseptert"),
    IKKE_LEVERT("Ikke levert"),
    MANULT_HANDTERT("Manuelt håndtert"),
    LEVERT_SDP("Levert SDP"),
    PRINTET("Printet"),
    SENDT_DIGITALT("Sendt digitalt"),
    SENDT_PRINT("Sendt print"),
    SENDT_SDP("Sendt SDP"),
    KLAR_FOR_MOTTAK("Klar for mottak"),
    VARSLET("Varslet");

    private String status;

    DpfReceiptStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatus() {
        return null;
    }
}
