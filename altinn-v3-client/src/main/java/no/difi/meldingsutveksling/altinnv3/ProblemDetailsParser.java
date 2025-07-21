package no.difi.meldingsutveksling.altinnv3;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.digdir.altinn3.correspondence.model.ProblemDetails;
import org.springframework.http.client.ClientHttpResponse;

import java.nio.charset.StandardCharsets;

public class ProblemDetailsParser {

    // parses the Altinn v3 Problem Details response, also see RFC7807
    // https://datatracker.ietf.org/doc/html/rfc7807

    public static String parseClientHttpResponse(String prefix, ClientHttpResponse response) {
        String jsonString = "Unknown Response";
        try {
            ObjectMapper mapper = new ObjectMapper();
            var body = response.getBody().readAllBytes();
            jsonString = new String(body, StandardCharsets.UTF_8);
            ProblemDetails problemDetails = mapper.readValue(body, ProblemDetails.class);
            return "%s: %d %s, %s".formatted(
                prefix,
                problemDetails.getStatus(),
                problemDetails.getTitle(),
                problemDetails.getDetail());
        } catch (Exception e) {
            return "Unable to parse as Altinn ProblemDetails: " + jsonString;
        }
    }

}
