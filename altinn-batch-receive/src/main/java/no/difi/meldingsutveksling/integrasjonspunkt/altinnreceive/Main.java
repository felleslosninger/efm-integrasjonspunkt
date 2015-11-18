package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsConfiguration;
import no.difi.meldingsutveksling.FileReference;
import org.apache.commons.cli.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive.AltinnOptionsValueConfigParser.getAltinnBatchImportOptions;
import static no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive.AltinnOptionsValueConfigParser.getAltinnWsClientConfiguration;

/**
 * Class responsible for downloading of Documents from the Altinn Webservice, and sending them to the
 * "Integrasjonspunkt"
 *
 * @author Glenn Bech
 */

public class Main {

    public static void main(String[] args) throws ParseException {

        Options helpOptions = new HelpCommandLineOptions();
        Options programOptions = new AltinnBatchCommandLineOptions();

        CommandLine commandLine = new DefaultParser().parse(helpOptions, args, true);
        if (commandLine.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("altinn batch import", null, programOptions, null);
            return;
        }

        programOptions = new AltinnBatchCommandLineOptions();
        commandLine = new DefaultParser().parse(programOptions, args);

        AltinnBatchImportConfiguration batchOptions = getAltinnBatchImportOptions(commandLine);
        final String integrasjonspunktEndPointURL = batchOptions.getIntegrasjonspunktEndPointURL();
        final String orgNumber = batchOptions.getOrganisationNumber();
        final int threadPoolSize = batchOptions.getThreadPoolSize();

        AltinnWsConfiguration altinnConfig = getAltinnWsClientConfiguration(commandLine);
        AltinnWsClient wsClient = new AltinnWsClient('altinnConfig);
        ReceiveClient receiveClient = new ReceiveClient(integrasjonspunktEndPointURL);

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        List<FileReference> files = wsClient.availableFiles(orgNumber);
        ReceiveClientContext ctx = new ReceiveClientContext(orgNumber, receiveClient, wsClient);

        for (FileReference file : files) {
            CallReceiveRunnable command = new CallReceiveRunnable(ctx, file);
            executor.execute(command);
        }
    }
}
