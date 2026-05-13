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

        assertEquals("""
            TestRun: {
                "type":"https://datatracker.ietf.org/doc/html/rfc7807",
                "title":"Unauthorized",
                "status":401,
                "detail":"You must use a bearer token that represents a system user with access to the resource in the Resource Rights Registry"
            }""", actual);

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

        assertEquals("""
            TestRun: {
                "type":"https://tools.ietf.org/html/rfc9110#section-15.5.2",
                "title":"Unauthorized",
                "status":401,
                "detail":"You must use a bearer token that represents a system user with access to the resource in the Resource Rights Registry",
                "traceId":"00-5f87f02bb2dac47bd8791b7e511ece2c-34c74bd580503f64-01"
            }""", actual);

    }

    @Test
    void parseProblemDetails_error_parsing_unknown_json() throws Exception {

        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("""
            {"unknown" : "value"}""".getBytes()));

        var actual = ProblemDetailsParser.parseClientHttpResponse("TestRun", clientHttpResponse);

        assertEquals("""
            TestRun: {"unknown" : "value"}""", actual);

    }

    @Test
    void parseProblemDetails_error_parsing_non_json() throws Exception {

        when(clientHttpResponse.getBody()).thenReturn(new ByteArrayInputStream("""
            NoJsonAtAll""".getBytes()));

        var actual = ProblemDetailsParser.parseClientHttpResponse("TestRun", clientHttpResponse);

        assertEquals( "TestRun: NoJsonAtAll", actual);

    }

}
