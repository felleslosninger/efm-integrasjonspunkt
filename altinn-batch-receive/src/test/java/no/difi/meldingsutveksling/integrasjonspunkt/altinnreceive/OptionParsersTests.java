package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.AltinnWsConfiguration;
import org.apache.commons.cli.ParseException;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive.AltinnOptionsValueConfigParser.*;

/**
 *
 */
public class OptionParsersTests {

    private static final String URL_WS_BROKER = "http://api.altinn.no/ws";
    private static final String URL_WS_STREAMING = "http://api.altinn.no/ws/streaming";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String URL_INTEGRASJONSPUNKT = "http://api.oslo.kommune/integrasjonspunkt";
    private static final String ORG_NUMBER = "011076999";

    @Test
    public void testAltinnOptionsParserTest() throws ParseException {

        String[] args = new String[8];
        int ix = 0;
        args[ix++] = "-a";
        args[ix++] = URL_WS_BROKER;

        args[ix++] = "-s";
        args[ix++] = URL_WS_STREAMING;

        args[ix++] = "-au";
        args[ix++] = USERNAME;

        args[ix++] = "-ap";
        args[ix++] = PASSWORD;

        AltinnWsConfiguration config = getAltinnWsClientConfiguration(args);
        assertEquals("-a option wrong", URL_WS_BROKER, config.getBrokerServiceUrl().toString());
        assertEquals("-s option wrong", URL_WS_STREAMING, config.getStreamingServiceUrl().toString());
        assertEquals("-au username", USERNAME, config.getUsername());
        assertEquals("-ap password", PASSWORD, config.getPassword());

    }

    @Test
    public void shouldParseProgramOptions() throws ParseException {

        String[] args = new String[4];
        int ix = 0;
        args[ix++] = "-i";
        args[ix++] = URL_INTEGRASJONSPUNKT;

        args[ix++] = "-o";
        args[ix++] = ORG_NUMBER;

        BatchOptions config = getBatchOptions(args);
        assertEquals("-i option is wrong", URL_INTEGRASJONSPUNKT, config.getIntegrasjonspunktEndPointURL());
        assertEquals("-o option is wrong", ORG_NUMBER, config.getOrganisationNumber());
    }


}
