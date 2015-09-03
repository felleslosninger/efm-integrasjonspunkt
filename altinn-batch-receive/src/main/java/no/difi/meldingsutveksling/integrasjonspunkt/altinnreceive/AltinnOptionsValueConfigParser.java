package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.AltinnWsConfiguration;
import org.apache.commons.cli.*;

import java.net.MalformedURLException;
import java.net.URL;

import static no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive.Constants.*;

/**
 * Class with a utility that creates a AltinnWSConfiguration from the command line given to the Java application
 *
 * @author Glenn Bech
 */
class AltinnOptionsValueConfigParser {

    private static final String OPTION_ALTINN_BROKER_SERVICE = "a";
    private static final String OPTION_ALTINN_STREAMING = "s";
    private static final String OPTION_ALTINN_USERNAME = "au";
    private static final String OPTION_ALTINN_PASSWORD = "ap";
    private static final String OPTION_ORGNUMBER = "o";
    private static final String OPTION_INTEGRASJONSPUNKT = "i";
    private static final String OPTION_THREAD_POOL_SIZE = "t";
    private static final String OPTION_HELP = "h";

    /**
     * Creates a AltinnWSConfiguration from the command line given to the Java application, using the Apache
     * commons CLI library
     *
     * @param args the value given to the public static void main() method of the Java Application
     * @return and AltinnWSConfiguration with values parsed from the command line arguments
     */

    public static AltinnWsConfiguration getAltinnWsClientConfiguration(String[] args) throws ParseException {
        CommandLine cmd = new DefaultParser().parse(new CliOptions(), args);

        URL urlBrokerService = getMandatoryURLOption(cmd, OPTION_ALTINN_BROKER_SERVICE);
        URL urlStreaming = getMandatoryURLOption(cmd, OPTION_ALTINN_STREAMING);
        String userName = cmd.getOptionValue(OPTION_ALTINN_USERNAME);
        String passord = cmd.getOptionValue(OPTION_ALTINN_PASSWORD);

        return new AltinnWsConfiguration.Builder().withBrokerServiceUrl(urlBrokerService)
                .withStreamingServiceUrl(urlStreaming)
                .withUsername(userName)
                .withPassword(passord).build();
    }

    public static AltinnBatchImportOptions getAltinnBatchImportOptions(String[] args) throws ParseException {
        CommandLine cmd = new DefaultParser().parse(new CliOptions(), args);
        URL ipUrl = getMandatoryURLOption(cmd, OPTION_INTEGRASJONSPUNKT);
        String orgNumber = cmd.getOptionValue(OPTION_ORGNUMBER);
        int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        String t = cmd.getOptionValue(OPTION_THREAD_POOL_SIZE);
        if (t != null) {
            threadPoolSize = Integer.parseInt(t);
        }
        return new AltinnBatchImportOptions(ipUrl.toString(), orgNumber, threadPoolSize);
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

    /**
     * There is no need for the world to see this, we hide it in here...
     */
    static class CliOptions extends Options {

        public CliOptions() {
            super();

            addOption(Option.builder(OPTION_ALTINN_BROKER_SERVICE).hasArg().required().build());
            addOption(Option.builder(OPTION_ALTINN_STREAMING).hasArg().required().build());
            addOption(Option.builder(OPTION_ALTINN_USERNAME).hasArg().required().build());
            addOption(Option.builder(OPTION_ALTINN_PASSWORD).hasArg().required().build());
            addOption(Option.builder(OPTION_ORGNUMBER).hasArg().required().build());
            addOption(Option.builder(OPTION_INTEGRASJONSPUNKT).hasArg().required().build());
            addOption(Option.builder(OPTION_THREAD_POOL_SIZE).hasArg().build());
            addOption(Option.builder(OPTION_HELP).build());
        }
    }
}
