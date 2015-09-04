package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

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

    private ReceiveClientContext ctx;
    private FileReference file;

    public CallReceiveRunnable(ReceiveClientContext ctx, FileReference file) {
        this.ctx = ctx;
        this.file = file;
    }

    @Override
    public void run() {
        DownloadRequest request = new DownloadRequest(file.getValue(), ctx.getOrgNr());
        StandardBusinessDocument doc = ctx.getAltinnWsClient().download(request);
        CorrelationInformation result = ctx.getReceiveClient().callReceive(new ModelMapper().map(doc, no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class));
        System.out.println(result.getRequestingDocumentInstanceIdentifier() + " sent ");
    }


}
