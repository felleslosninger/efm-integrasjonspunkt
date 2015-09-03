package no.difi.meldingsutveksling.integrasjonspunkt.altinnreceive;

import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;

import javax.xml.ws.BindingProvider;

/**
 * This class is responsible for involing the web service methos "receive" on the
 * "integrasjonspunkt" receiving the document
 *
 * @author Glenn Bech
 */
class Receive {

    private SOAReceivePort port;

    public Receive(String endPointURL) {
        no.difi.meldingsutveksling.noarkexchange.schema.receive.Receive exchange = new no.difi.meldingsutveksling.noarkexchange.schema.receive.Receive();
        port = exchange.getReceivePort();
        BindingProvider bp = (BindingProvider) port;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPointURL);
    }

    public CorrelationInformation callReceive(StandardBusinessDocument sbd) {
        return port.receive(sbd);
    }

}
