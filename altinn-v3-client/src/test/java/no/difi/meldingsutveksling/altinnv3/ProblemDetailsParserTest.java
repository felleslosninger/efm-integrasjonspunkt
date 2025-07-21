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
                    "type":"https://tools.ietf.org/html/rfc9110#section-15.5.2",
                    "title":"Unauthorized",
                    "status":401,
                    "detail":"You must use a bearer token that represents a system user with access to the resource in the Resource Rights Registry"
                }""".getBytes()));

        var actual = ProblemDetailsParser.parseClientHttpResponse("TestRun", clientHttpResponse);

        assertEquals("TestRun: 401 Unauthorized, You must use a bearer token that represents a system user with access to the resource in the Resource Rights Registry", actual);

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

        assertEquals("TestRun: 401 Unauthorized, You must use a bearer token that represents a system user with access to the resource in the Resource Rights Registry", actual);

    }

    @Test
    void parseProblemDetails_error_parsing_unknown_json() throws Exception {

        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("""
            {"unknown" : "value"}""".getBytes()));

        var actual = ProblemDetailsParser.parseClientHttpResponse("TestRun", clientHttpResponse);

        assertEquals("""
            TestRun: null No title was given, Response was : {"unknown" : "value"}""", actual);

    }

    @Test
    void parseProblemDetails_error_parsing_non_json() throws Exception {

        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("""
                NoJsonAtAll""".getBytes()));

        var actual = ProblemDetailsParser.parseClientHttpResponse("TestRun", clientHttpResponse);

        assertEquals("""
            Unable to parse as Altinn ProblemDetails: Unrecognized token 'NoJsonAtAll': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
             at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 12](NoJsonAtAll)""", actual);

    }

}
