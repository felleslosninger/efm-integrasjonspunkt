package no.difi.meldingsutveksling.knutepunkrecieve;

import javax.xml.ws.BindingProvider;

import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;

public class Receive {
	private SOAReceivePort port;

	public Receive(String endPointURL) {
		no.difi.meldingsutveksling.noarkexchange.schema.receive.Receive exchange = new no.difi.meldingsutveksling.noarkexchange.schema.receive.Receive();
		port = exchange.getReceivePort();
		BindingProvider bp = (BindingProvider) port;
		bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPointURL);
	}

	public CorrelationInformation sendEduMeldig(StandardBusinessDocument sbd) {
		return port.receive(sbd);
	}

}
