package no.difi.meldingsutveksling.altinnv3.token;

import no.difi.meldingsutveksling.config.AltinnSystemUser;
import no.difi.move.common.oauth.JwtTokenAdditionalClaims;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClaimsFactory {

    public static final String ISO_6523_ACTORID_UPIS = "iso6523-actorid-upis";
    public static final String URN_ALTINN_SYSTEMUSER = "urn:altinn:systemuser";

    public static JwtTokenAdditionalClaims getAuthorizationClaims(AltinnSystemUser systemUser) {
        var claims = new HashMap<String, Object>();

        Map<String, Object> systemuserOrg = new HashMap<>();
        systemuserOrg.put("authority", ISO_6523_ACTORID_UPIS);
        systemuserOrg.put("ID", systemUser.getOrgId());

        Map<String, Object> authDetail = new HashMap<>();
        authDetail.put("systemuser_org", systemuserOrg);
        authDetail.put("type", URN_ALTINN_SYSTEMUSER);
        authDetail.put("externalRef", systemUser.getName());

        claims.put("authorization_details", List.of(authDetail));

        var result = new JwtTokenAdditionalClaims();
        result.setClaims(claims);
        return result;
    }

}
