package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.AltinnWsConfiguration;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive.AltinnOptionsValueConfigParser.getAltinnBatchImportOptions;
import static no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive.AltinnOptionsValueConfigParser.getAltinnWsClientConfiguration;

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
    private static final int THREAD_POOL_SIZE = 10;
    CommandLine commandLine ;

    @Before
    public void setupArguments() throws ParseException {
        String[] args;
        args = new String[14];
        int i = 0;
        args[i++] = "-a";
        args[i++] = URL_WS_BROKER;

        args[i++] = "-s";
        args[i++] = URL_WS_STREAMING;

        args[i++] = "-au";
        args[i++] = USERNAME;

        args[i++] = "-ap";
        args[i++] = PASSWORD;

        args[i++] = "-i";
        args[i++] = URL_INTEGRASJONSPUNKT;

        args[i++] = "-o";
        args[i++] = ORG_NUMBER;

        args[i++] = "-t";
        args[i++] = "10";

        commandLine = new DefaultParser().parse(new AltinnBatchCommandLineOptions(), args);

    }

    @Test
    public void testCreateAltinnWsconfig() throws ParseException {
        AltinnWsConfiguration config = getAltinnWsClientConfiguration(commandLine);
        assertEquals("-a option wrong", URL_WS_BROKER, config.getBrokerServiceUrl().toString());
        assertEquals("-s option wrong", URL_WS_STREAMING, config.getStreamingServiceUrl().toString());
        assertEquals("-au username", USERNAME, config.getUsername());
        assertEquals("-ap password", PASSWORD, config.getPassword());

    }

    @Test
    public void shouldParseProgramOptions() throws ParseException {
        AltinnBatchImportConfiguration config = getAltinnBatchImportOptions(commandLine);
        assertEquals("-i option is wrong", URL_INTEGRASJONSPUNKT, config.getIntegrasjonspunktEndPointURL());
        assertEquals("-o option is wrong", ORG_NUMBER, config.getOrganisationNumber());
        assertEquals("-t option is wrong", THREAD_POOL_SIZE, config.getThreadPoolSize());
    }
}

