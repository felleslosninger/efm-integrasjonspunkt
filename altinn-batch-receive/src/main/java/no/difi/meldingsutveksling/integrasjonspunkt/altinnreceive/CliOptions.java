package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 */
public class CliOptions extends Options {
    public CliOptions() {
        super();

        OptionBuilder.hasArg().withDescription("URL for the AltinnBrokerService").withLongOpt("integrasjonspunkt");
        addOption(OptionBuilder.create("a"));

        OptionBuilder.hasArg().withDescription("URL for the AltinnBrokerService for Streaming").withLongOpt("integrasjonspunkt");
        addOption(OptionBuilder.create("s"));

        OptionBuilder.hasArg().withDescription("URL for the integrasjonspunkt ").withLongOpt("integrasjonspunkt");
        addOption(OptionBuilder.create("i"));

        OptionBuilder.withDescription("print this message").withLongOpt("help");
        addOption(OptionBuilder.create("h"));


    }
}