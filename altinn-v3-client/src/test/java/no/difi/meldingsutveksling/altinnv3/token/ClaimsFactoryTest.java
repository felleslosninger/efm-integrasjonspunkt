package no.difi.meldingsutveksling.altinnv3.token;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import no.difi.meldingsutveksling.config.AltinnSystemUser;
import no.difi.move.common.oauth.JwtTokenAdditionalClaims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ClaimsFactoryTest {

    @Test
    @DisplayName("Should create authorization claims from values set in properties")
    void shouldReturnAuthorizationClaims() {
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

        ObjectMapper mapper = new JsonMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);

        assertThat(expectedResultInJsonFormat)
            .describedAs("Generated claims should be the same as expected result")
            .isEqualToIgnoringNewLines(json);
    }

}
