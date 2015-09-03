package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsConfiguration;
import no.difi.meldingsutveksling.FileReference;
import org.apache.commons.cli.ParseException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive.AltinnOptionsValueConfigParser.getAltinnWsClientConfiguration;
import static no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive.AltinnOptionsValueConfigParser.getAltinnBatchImportOptions;

/**
 * Class responsible for downloading of Documents from the Altinn Webservice, and sending them to the
 * "Integrasjonspunkt"
 *
 * @author Glenn Bech
 */

public class Main {

    public static void main(String[] args) throws ParseException {

        AltinnBatchImportOptions batchOptions = getAltinnBatchImportOptions(args);
        final String integrasjonspunktEndPointURL = batchOptions.getIntegrasjonspunktEndPointURL();
        final String orgNumber = batchOptions.getOrganisationNumber();
        final int threadPoolSize = batchOptions.getThreadPoolSize();

        AltinnWsConfiguration altinnConfig = getAltinnWsClientConfiguration(args);
        AltinnWsClient wsClient = new AltinnWsClient(altinnConfig);
        Receive receive = new Receive(integrasjonspunktEndPointURL);

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        List<FileReference> files = wsClient.availableFiles(orgNumber);

        for (FileReference file : files) {
            CallReceiveRunnable command = new CallReceiveRunnable(wsClient, receive, file, orgNumber);
            executor.execute(command);
        }
    }
}
