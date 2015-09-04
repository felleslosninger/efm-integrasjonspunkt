package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.AltinnWsClient;

/**
 *
 */
public class ReceiveClientContext {

    private AltinnWsClient altinnWsClient;
    private ReceiveClient receiveClient;
    private String orgNr;

    public ReceiveClientContext(String orgNr, ReceiveClient receiveClient, AltinnWsClient altinnWsClient) {
        this.orgNr = orgNr;
        this.receiveClient = receiveClient;
        this.altinnWsClient = altinnWsClient;
    }

    public AltinnWsClient getAltinnWsClient() {
        return altinnWsClient;
    }

    public ReceiveClient getReceiveClient() {
        return receiveClient;
    }

    public String getOrgNr() {
        return orgNr;
    }
}
