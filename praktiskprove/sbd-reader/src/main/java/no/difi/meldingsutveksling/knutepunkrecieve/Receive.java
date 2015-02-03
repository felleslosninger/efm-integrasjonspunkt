package no.difi.meldingsutveksling.knutepunkrecieve;

import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;

import javax.xml.ws.BindingProvider;

public class Receive {
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
