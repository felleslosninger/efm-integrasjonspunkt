package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.AltinnWsClient;
import no.difi.meldingsutveksling.DownloadRequest;
import no.difi.meldingsutveksling.FileReference;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import org.modelmapper.ModelMapper;

/**
 * Runnable wrapper for web service calls to the integrasjonspunkt receiving documents from Altinn
 * Enables thread pooling / paralell execution
 *
 * @author Glenn Bech
 */
class CallReceiveRunnable implements Runnable {

    private AltinnWsClient wsClient;
    private Receive receive;
    private FileReference file;
    private String orgNumber;

    public CallReceiveRunnable(AltinnWsClient wsClient, Receive receive, FileReference file, String orgNumber) {
        this.wsClient = wsClient;
        this.receive = receive;
        this.file = file;
        this.orgNumber = orgNumber;
    }

    @Override
    public void run() {
        DownloadRequest request = new DownloadRequest(file.getValue(), orgNumber);
        StandardBusinessDocument doc = wsClient.download(request);
        CorrelationInformation result = receive.callReceive(new ModelMapper().map(doc, no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class));
        System.out.println(result.getRequestingDocumentInstanceIdentifier() + " sent ");
    }
}
