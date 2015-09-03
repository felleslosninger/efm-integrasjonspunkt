package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.AltinnWsConfiguration;
import org.apache.commons.cli.*;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Class with a utility that creates a AltinnWSConfiguration from the command line given to the Java application
 */
public class AltinnOptionsValueConfigParser {

    public static final String OPTION_INTEGRASJONSPUNKT = "i";
    public static final String OPTION_ALTINN_BROKER_SERVICE = "a";
    public static final String OPTION_ALTINN_STREAMING = "s";
    public static final String OPTION_ALTINN_USERNAME = "au";
    public static final String OPTION_ALTINN_PASSWORD = "ap";
    public static final String OPTION_HELP = "h";

    /**
     * Creates a AltinnWSConfiguration from the command line given to the Java application, using the Apache
     * commons CLI library
     *
     * @param args the value given to the public static void main() method of the Java Application
     * @return and AltinnWSConfiguration with values parsed from the command line arguments
     */

    public static AltinnWsConfiguration getConfiguration(String[] args) throws ParseException {
        CommandLine cmd = new PosixParser().parse(new CliOptions(), args);

        URL urlBrokerService = parseURLOption(cmd, OPTION_ALTINN_BROKER_SERVICE);
        URL urlStreaming = parseURLOption(cmd, OPTION_ALTINN_STREAMING);
        String userName = cmd.getOptionValue(OPTION_ALTINN_USERNAME);
        String passord = cmd.getOptionValue(OPTION_ALTINN_PASSWORD);

        return new AltinnWsConfiguration.Builder().withBrokerServiceUrl(urlBrokerService)
                .withStreamingServiceUrl(urlStreaming)
                .withUsername(userName)
                .withPassword(passord).build();
    }

    private static URL parseURLOption(CommandLine cmd, String optionName) throws ParseException {
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

            OptionBuilder.hasArg().withDescription("URL for the AltinnBrokerService").withLongOpt("integrasjonspunkt");
            addOption(OptionBuilder.create(OPTION_ALTINN_BROKER_SERVICE));

            OptionBuilder.hasArg().withDescription("URL for the AltinnBrokerService for Streaming").withLongOpt("integrasjonspunkt");
            addOption(OptionBuilder.create(OPTION_ALTINN_STREAMING));

            OptionBuilder.hasArg().withDescription("URL for the Altinn services username").withLongOpt("integrasjonspunkt");
            addOption(OptionBuilder.create(OPTION_ALTINN_USERNAME));

            OptionBuilder.hasArg().withDescription("URL for the Altinn services password").withLongOpt("integrasjonspunkt");
            addOption(OptionBuilder.create(OPTION_ALTINN_PASSWORD));

            OptionBuilder.hasArg().withDescription("URL for the integrasjonspunkt ").withLongOpt("integrasjonspunkt");
            addOption(OptionBuilder.create(OPTION_INTEGRASJONSPUNKT));

            OptionBuilder.withDescription("print this message").withLongOpt("help");
            addOption(OptionBuilder.create(OPTION_HELP));


        }
    }
}
