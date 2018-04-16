package no.difi.meldingsutveksling.ks.svarut;

import lombok.Data;

@Data
public class SvarUtRequest {
    private final String endPointURL;
    private final SendForsendelseMedId forsendelse;

    public SvarUtRequest(String endPointURL, SendForsendelseMedId forsendelse) {
        this.endPointURL = endPointURL;
        this.forsendelse = forsendelse;
    }
}
