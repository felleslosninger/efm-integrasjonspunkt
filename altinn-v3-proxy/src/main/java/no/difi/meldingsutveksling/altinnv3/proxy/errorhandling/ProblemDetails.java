package no.difi.meldingsutveksling.altinnv3.proxy.errorhandling;

import java.nio.charset.StandardCharsets;

public class ProblemDetails {

    public static byte[] createProblemDetailsAsByteArray(String title, int status, String detail) {
        var problemDetails = """
            {
                "type":"https://tools.ietf.org/html/rfc7807",
                "title":"%s",
                "status":%d,
                "detail":"%s"
            }
            """.formatted(title, status, detail);
        return problemDetails.getBytes(StandardCharsets.UTF_8);
   }

}
