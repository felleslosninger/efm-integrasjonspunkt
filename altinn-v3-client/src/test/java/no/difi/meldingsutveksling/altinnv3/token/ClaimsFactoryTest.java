package no.difi.meldingsutveksling.altinnv3.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.difi.meldingsutveksling.config.AltinnSystemUser;
import no.difi.move.common.oauth.JwtTokenAdditionalClaims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClaimsFactoryTest {

    @Test
    @DisplayName("Should create authorization claims from values set in properties")
    void shouldReturnAuthorizationClaims() throws JsonProcessingException {
        String expectedResultInJsonFormat = """
        {
          "claims" : {
            "authorization_details" : [ {
              "systemuser_org" : {
                "authority" : "iso6523-actorid-upis",
                "ID" : "0192:311780735"
              },
              "type" : "urn:altinn:systemuser",
              "externalRef" : "externalref"
            } ]
          }
        }""";

        var systemUser = new AltinnSystemUser();
        systemUser.setOrgId("0192:311780735");
        systemUser.setName("externalref");

        var result = ClaimsFactory.getAuthorizationClaims(systemUser);

        assertNotNull(result, "Should get authorization claims");
        assertInstanceOf(JwtTokenAdditionalClaims.class, result, "Generated claims should be JwtTokenAdditionalClaims");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);

        assertEquals(expectedResultInJsonFormat, json, "Generated claims should be the same as expected result");
    }

}
