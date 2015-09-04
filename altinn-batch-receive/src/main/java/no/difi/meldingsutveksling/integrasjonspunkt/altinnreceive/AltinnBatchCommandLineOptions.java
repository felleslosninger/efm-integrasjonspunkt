package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class AltinnBatchCommandLineOptions extends Options {

    public static final String OPTION_ALTINN_BROKER_SERVICE = "a";
    public static final String OPTION_ALTINN_STREAMING = "s";
    public static final String OPTION_ALTINN_USERNAME = "au";
    public static final String OPTION_ALTINN_PASSWORD = "ap";
    public static final String OPTION_ORGNUMBER = "o";
    public static final String OPTION_INTEGRASJONSPUNKT = "i";
    public static final String OPTION_THREAD_POOL_SIZE = "t";


    public AltinnBatchCommandLineOptions() {
        super();

        addOption(Option.builder(OPTION_ALTINN_BROKER_SERVICE)
                .hasArg()
                .desc("URL til AltinnBrokerService")
                .required()
                .build());

        addOption(Option.builder(OPTION_ALTINN_STREAMING)
                .desc("URL til AltinnBrokerService for Streaming")
                .hasArg()
                .required()
                .build());

        addOption(Option.builder(OPTION_ALTINN_USERNAME)
                .desc("Altinn WS brukernavn")
                .hasArg()
                .required()
                .build());
        addOption(Option.builder(OPTION_ALTINN_PASSWORD)
                .desc("ALtinn WS passord")
                .hasArg()
                .required()
                .build());

        addOption(Option.builder(OPTION_ORGNUMBER)
                .desc("Organisasjonsnummer")
                .hasArg()
                .required()
                .build());

        addOption(Option.builder(OPTION_INTEGRASJONSPUNKT)
                .desc("Full URL til integrasjonspunkt sitt /receive endepunkt")
                .hasArg()
                .required()
                .build());

        addOption(Option.builder(OPTION_THREAD_POOL_SIZE)
                .desc("Maksimalt antall samtidige operasjoner")
                .hasArg()
                .build());

    }
}
