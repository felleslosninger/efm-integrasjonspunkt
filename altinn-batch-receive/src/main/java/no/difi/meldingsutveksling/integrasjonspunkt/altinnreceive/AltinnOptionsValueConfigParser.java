package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.AltinnWsConfiguration;
import org.apache.commons.cli.*;

import java.net.MalformedURLException;
import java.net.URL;

import static no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive.AltinnBatchCommandLineOptions.*;
import static no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive.Constants.DEFAULT_THREAD_POOL_SIZE;

/**
 * Class with a utility that creates a AltinnWSConfiguration from the command line given to the Java application
 *
 * @author Glenn Bech
 */
class AltinnOptionsValueConfigParser {

    /**
     * Creates a AltinnWSConfiguration from the command line given to the Java application, using the Apache
     * commons CLI library
     *
     * @param cmd the value given to the public static void main() method of the Java Application
     * @return and AltinnWSConfiguration with values parsed from the command line arguments
     */

    public static AltinnWsConfiguration getAltinnWsClientConfiguration(CommandLine cmd) throws ParseException {

        URL urlBrokerService = getMandatoryURLOption(cmd, OPTION_ALTINN_BROKER_SERVICE);
        URL urlStreaming = getMandatoryURLOption(cmd, OPTION_ALTINN_STREAMING);
        String userName = cmd.getOptionValue(OPTION_ALTINN_USERNAME);
        String passord = cmd.getOptionValue(OPTION_ALTINN_PASSWORD);

        return new AltinnWsConfiguration.Builder()
                .withBrokerServiceUrl(urlBrokerService)
                .withStreamingServiceUrl(urlStreaming)
                .withUsername(userName)
                .withPassword(passord).build();
    }

    /**
     * Creates a AltinnBatchImportConfiguration from the command line given to the Java application, using the Apache
     * commons CLI library. The AltinnBatchImportConfiguration are the arguments that decides how the application is run.
     *
     * @param cmd the value given to the public static void main() method of the Java Application
     * @return and AltinnWSConfiguration with values parsed from the command line arguments
     */
    public static AltinnBatchImportConfiguration getAltinnBatchImportOptions(CommandLine cmd) throws ParseException {

        URL ipUrl = getMandatoryURLOption(cmd, OPTION_INTEGRASJONSPUNKT);
        String orgNumber = cmd.getOptionValue(OPTION_ORGNUMBER);
        int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        String t = cmd.getOptionValue(OPTION_THREAD_POOL_SIZE);
        if (t != null) {
            threadPoolSize = Integer.parseInt(t);
        }
        return new AltinnBatchImportConfiguration(ipUrl.toString(), orgNumber, threadPoolSize);
    }

    private static URL getMandatoryURLOption(CommandLine cmd, String optionName) throws ParseException {
        URL urlBrokerService;
        String stringValue = cmd.getOptionValue(optionName);
        try {
            urlBrokerService = new URL(stringValue);
        } catch (MalformedURLException e) {
            throw new ParseException(stringValue + " is a malformed URL");
        }
        return urlBrokerService;
    }

}

