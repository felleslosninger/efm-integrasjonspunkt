package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;


import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class HelpCommandLineOptions extends Options {

    public static final String OPTION_HELP = "h";

    public HelpCommandLineOptions() {
        super();
        addOption(Option.builder(OPTION_HELP)
                .desc("Prints this help message")
                .build());
    }
}

