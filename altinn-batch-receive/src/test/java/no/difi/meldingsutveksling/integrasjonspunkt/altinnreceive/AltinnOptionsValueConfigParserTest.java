package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.AltinnWsConfiguration;
import org.apache.commons.cli.ParseException;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class AltinnOptionsValueConfigParserTest {


    public static final String URL_WS_BROKER = "http://api.altinn.no/ws";
    public static final String URL_WS_STREAMING = "http://api.altinn.no/ws/streaming";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String URL_INTEGRASJONSPUNKT = "http://api.oslo.kommune/integrasjonspunkt";

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

        AltinnWsConfiguration config = AltinnOptionsValueConfigParser.getConfiguration(args);
        assertEquals("-a option wrong", URL_WS_BROKER, config.getBrokerServiceUrl().toString());
        assertEquals("-s option wrong", URL_WS_STREAMING, config.getStreamingServiceUrl().toString());
        assertEquals("-au username", USERNAME, config.getUsername());
        assertEquals("-ap password", PASSWORD, config.getPassword());

    }


}
