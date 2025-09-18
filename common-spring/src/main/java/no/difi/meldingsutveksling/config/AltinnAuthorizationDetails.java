package no.difi.meldingsutveksling.config;

import lombok.Data;

@Data
public class AltinnAuthorizationDetails {

    private String systemuserOrgId;  // format : 0192:311780735
    private String externalRef;      // eks : 311780735_integrasjonspunkt_systembruker_test

}
