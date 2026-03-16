package no.difi.meldingsutveksling.altinnv3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemDetailsParserTest {

    @Mock
    ClientHttpResponse clientHttpResponse;

    @Test
    void parseProblemDetails_ok() throws Exception {

        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("""
                {
                    "type":"https://datatracker.ietf.org/doc/html/rfc7807",
                    "title":"Unauthorized",
                    "status":401,
                    "detail":"You must use a bearer token that represents a system user with access to the resource in the Resource Rights Registry"
                }""".getBytes()));

        var actual = ProblemDetailsParser.parseClientHttpResponse("TestRun", clientHttpResponse);

        assert actual.startsWith("TestRun: 401 Unauthorized, You must use a bearer token that represents a system user with access to the resource in the Resource Rights Registry");
        assert actual.contains("(raw: ");

    }

    @Test
    void parseProblemDetails_ok_with_unknown_property_traceId() throws Exception {

        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("""
                {
                    "type":"https://tools.ietf.org/html/rfc9110#section-15.5.2",
                    "title":"Unauthorized",
                    "status":401,
                    "detail":"You must use a bearer token that represents a system user with access to the resource in the Resource Rights Registry",
                    "traceId":"00-5f87f02bb2dac47bd8791b7e511ece2c-34c74bd580503f64-01"
                }""".getBytes()));

        var actual = ProblemDetailsParser.parseClientHttpResponse("TestRun", clientHttpResponse);

        assert actual.startsWith("TestRun: 401 Unauthorized, You must use a bearer token that represents a system user with access to the resource in the Resource Rights Registry");
        assert actual.contains("traceId");

    }

    @Test
    void parseProblemDetails_includes_raw_body_with_errors_field() throws Exception {

        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("""
                {
                    "title":"One or more validation errors occurred.",
                    "status":400,
                    "errors":{"Correspondence.Content.Language":["The field Language must be a string with a minimum length of 2"]}
                }""".getBytes()));

        var actual = ProblemDetailsParser.parseClientHttpResponse("TestRun", clientHttpResponse);

        assert actual.contains("One or more validation errors occurred.");
        assert actual.contains("Correspondence.Content.Language");
        assert actual.contains("The field Language must be a string with a minimum length of 2");

    }

    @Test
    void parseProblemDetails_error_parsing_unknown_json() throws Exception {

        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("""
            {"unknown" : "value"}""".getBytes()));

        var actual = ProblemDetailsParser.parseClientHttpResponse("TestRun", clientHttpResponse);

        assert actual.contains("No title was given");
        assert actual.contains("unknown");

    }

    @Test
    void parseProblemDetails_error_parsing_non_json() throws Exception {

        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("""
                NoJsonAtAll""".getBytes()));

        var actual = ProblemDetailsParser.parseClientHttpResponse("TestRun", clientHttpResponse);

        assert actual.contains("Unable to parse as Altinn ProblemDetails");
        assert actual.contains("NoJsonAtAll");

    }

}
