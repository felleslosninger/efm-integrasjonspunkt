package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.AltinnWsConfiguration;
import no.difi.meldingsutveksling.DownloadRequest;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceAvailableFile;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.modelmapper.ModelMapper;

import java.util.List;

import static no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive.AltinnOptionsValueConfigParser.getAltinnWsClientConfiguration;
import static no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive.AltinnOptionsValueConfigParser.getBatchOptions;

/**
 * Class responsible for downloading of Documents from the Altinn Webservice, and sending them to the
 * "Integrasjonspunkt"
 *
 * @author Glenn Bech
 */

public class Main {

    private static CliOptions options;
    private static CommandLine cmd;

    public static void main(String[] args) throws ParseException {

        BatchOptions batchOptions = getBatchOptions(args);
        String integrasjonspunktEndPointURL = batchOptions.getIntegrasjonspunktEndPointURL();
        String orgNumber = batchOptions.getOrganisationNumber();
        batchOptions.getIntegrasjonspunktEndPointURL();

        Receive receive = new Receive(integrasjonspunktEndPointURL);

        ModelMapper mapper = new ModelMapper();
        AltinnWsConfiguration config = getAltinnWsClientConfiguration(args);
        AltinnWsClient wsClient = new AltinnWsClient(config);

        List<BrokerServiceAvailableFile> files = wsClient.availableFiles(orgNumber);

        for (BrokerServiceAvailableFile file : files) {
            StandardBusinessDocument doc =
                    wsClient.download(new DownloadRequest(file.getFileReference(), orgNumber));
            receive.callReceive(mapper.map(doc, no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class))
        }

    }
}
