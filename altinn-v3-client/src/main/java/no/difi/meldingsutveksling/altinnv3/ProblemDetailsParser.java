package no.difi.meldingsutveksling.altinnv3;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.digdir.altinn3.correspondence.model.ProblemDetails;
import org.springframework.http.client.ClientHttpResponse;

import java.nio.charset.StandardCharsets;

public class ProblemDetailsParser {

    // parses the Altinn v3 Problem Details response, also see RFC7807
    // https://datatracker.ietf.org/doc/html/rfc7807

    public static String parseClientHttpResponse(String prefix, ClientHttpResponse response) {
        var jsonAsString = "NoProblemDetails";
        try {
            // FIXME this is only while debugging manually, should be removed later
            var responseStatus = response.getStatusCode();
            var responseStatusCode = response.getStatusText();
            System.out.printf("%s: %d %s%n", prefix, responseStatus.value(), responseStatusCode);
        } catch (Exception e) { /* ignore */ }
        try {
            var objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            var bodyAsBytes = response.getBody().readAllBytes();
            jsonAsString = new String(bodyAsBytes, StandardCharsets.UTF_8);
            var problemDetails = objectMapper.readValue(jsonAsString, ProblemDetails.class);
            if (problemDetails.getTitle() == null) problemDetails.setTitle("No title was given");
            if (problemDetails.getDetail() == null) problemDetails.setDetail("Response was : " + jsonAsString);
            return "%s: %d %s, %s".formatted(
                prefix,
                problemDetails.getStatus(),
                problemDetails.getTitle(),
                problemDetails.getDetail()
            );
        } catch (Exception e) {
            return "Unable to parse as Altinn ProblemDetails: " + e.getMessage() + "(" + jsonAsString + ")";
        }
    }

}
