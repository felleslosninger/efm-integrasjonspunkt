package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 */
public class CliOptions extends Options {
    public CliOptions() {
        super();

        OptionBuilder.hasOptionalArg().withDescription("The folder from which to read XMLs").withLongOpt("inputdirectiory");
        addOption(OptionBuilder.create("d"));

        OptionBuilder.hasArg().withDescription("URL for the endpoint call").withLongOpt("integrasjonspunkt");
        addOption(OptionBuilder.create("i"));

        OptionBuilder.withDescription("print this message").withLongOpt("help");
        addOption(OptionBuilder.create("h"));

        OptionBuilder.withDescription("Recursively go trough sub-folders");
        addOption(OptionBuilder.create("r"));

    }
}