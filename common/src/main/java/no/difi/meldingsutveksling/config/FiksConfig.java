package no.difi.meldingsutveksling.config;

import lombok.Data;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.net.URL;

@Data
public class FiksConfig {
    private boolean kryptert = true;

    @Valid
    @NotNull(message = "Certificate properties for FIKS not set.")
    private KeyStoreProperties keystore;

    SvarUt ut = new SvarUt();
    SvarInn inn = new SvarInn();

    @Data
    @ToString(exclude = "password")
    public static class SvarUt {
        @Valid()
        @Pattern(regexp = "^[a-zA-Z0-9\\-\\.øæåØÆÅ]{0,20}$")
        private String konverteringsKode;
        private String username;
        private String password;
        private URL endpointUrl;
    }

    @Data
    @ToString(exclude = "password")
    public static class SvarInn {
        @NotNull
        private String baseUrl;
        private String username;
        private String password;
        private boolean mailOnError;
        private String mailSubject;
        private String fallbackSenderOrgNr;
    }
}
