package no.difi.meldingsutveksling.config;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

@Data
public class SvarUtConfig {
    private boolean kryptert = true;

    @Valid()
    @Pattern(regexp = "^[a-zA-Z0-9\\-\\.øæåØÆÅ]{0,20}$")
    private String konverteringsKode;
}
