package no.difi.meldingsutveksling.altinnv3.token;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO : Denne kan nok flyttes til common når alt virker
public class AdditionalClaims {

    public Map<String, Object> getClaims() {

        var claims = new HashMap<String, Object>();

        Map<String, Object> systemuserOrg = new HashMap<>();
        systemuserOrg.put("authority", "iso6523-actorid-upis");
        systemuserOrg.put("ID", "0192:991825827");

        Map<String, Object> authDetail = new HashMap<>();
        authDetail.put("systemuser_org", systemuserOrg);
        authDetail.put("type", "urn:altinn:systemuser");
        authDetail.put("externalRef", "991825827_integrasjonspunkt_systembruker_test");

        claims.put("authorization_details", List.of(authDetail));

        return claims;

        /*
          "authorization_details" : [ {
            "systemuser_org" : {
              "authority" : "iso6523-actorid-upis",
              "ID" : "0192:311780735"
            },
            "type" : "urn:altinn:systemuser",
            "externalRef" : "311780735_integrasjonspunkt_systembruker_test"
          } ],
         */

    }

}
