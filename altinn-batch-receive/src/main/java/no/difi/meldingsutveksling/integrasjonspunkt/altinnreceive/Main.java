package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

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
        options = new CliOptions();
        cmd = new PosixParser().parse(options, args);



        AltinnCon
    }

}
