package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsConfiguration;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;

/**
 * Class responsible for downloading SBD Documents from the Altinn Serivce, and sending the to the
 * "Integrasjonspunkt"
 *
 * @author Glenn Bech
 */

public class Main {

    private static CliOptions options;
    private static CommandLine cmd;

    public static void main(String[] args) throws ParseException {

        AltinnWsConfiguration config = AltinnOptionsValueConfigParser.getConfiguration(args);
        AltinnWsClient wsClient = new AltinnWsClient(config);
        wsClient.availableFiles();

    }


}
