package no.difi.meldingsutveksling.config;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class FiksConfig {
    private boolean kryptert = true;

    @Valid
    @NotNull(message = "Certificate properties for FIKS not set.")
    private IntegrasjonspunktProperties.Keystore keystore;

    SvarUt ut = new SvarUt();
    SvarInn inn = new SvarInn();

    @Data
    public static class SvarUt {
        @Valid()
        @Pattern(regexp = "^[a-zA-Z0-9\\-\\.øæåØÆÅ]{0,20}$")
        private String konverteringsKode;

        private String username;

        private String password;
    }

    @Data
    public static class SvarInn {
        @NotNull
        private String baseUrl;

        private String username;

        private String password;
    }
}
