package no.difi.meldingsutveksling.config;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AltinnSystemUser {

    /**
     * format : 0192:311780735
     */
    @NotNull
    private String orgId;
    /**
     * eks : 311780735_integrasjonspunkt_systembruker_test
     */
    @NotNull
    private String name;

}
