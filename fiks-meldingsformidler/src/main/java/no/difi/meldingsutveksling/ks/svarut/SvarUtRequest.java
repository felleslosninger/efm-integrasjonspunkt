package no.difi.meldingsutveksling.ks.svarut;

import lombok.Data;

@Data
public class SvarUtRequest {
    private final String endPointURL;
    private final Forsendelse forsendelse;

    public SvarUtRequest(String endPointURL, Forsendelse forsendelse) {
        this.endPointURL = endPointURL;
        this.forsendelse = forsendelse;
    }
}
