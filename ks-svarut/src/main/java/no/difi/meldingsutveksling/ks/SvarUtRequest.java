package no.difi.meldingsutveksling.ks;

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
