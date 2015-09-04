package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;

import javax.xml.ws.BindingProvider;

/**
 * This class is responsible for calleing the web service method "receive" on the
 * "integrasjonspunkt" that is receiving the document
 *
 * @author Glenn Bech
 */
class ReceiveClient {

    private SOAReceivePort port;

    public ReceiveClient(String endPointURL) {
        no.difi.meldingsutveksling.noarkexchange.schema.receive.Receive exchange = new no.difi.meldingsutveksling.noarkexchange.schema.receive.Receive();
        port = exchange.getReceivePort();
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPointURL);
    }

    public CorrelationInformation callReceive(StandardBusinessDocument sbd) {
        return port.receive(sbd);
    }

}
