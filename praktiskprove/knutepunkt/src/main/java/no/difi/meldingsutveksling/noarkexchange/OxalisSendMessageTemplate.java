package no.difi.meldingsutveksling.noarkexchange;

import eu.peppol.outbound.OxalisOutboundModule;
import eu.peppol.outbound.transmission.TransmissionRequest;
import eu.peppol.outbound.transmission.TransmissionRequestBuilder;
import eu.peppol.outbound.transmission.Transmitter;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD.marshal;

@Component
@Profile("dev")
public class OxalisSendMessageTemplate extends SendMessageTemplateImpl {

	@Override
	void sendSBD(StandardBusinessDocument sbd) {

		sbd.getStandardBusinessDocumentHeader().getBusinessScope().getScope().addAll(createOxalisSpecificScopes());

		OxalisOutboundModule oxalisOutboundModule = new OxalisOutboundModule();

		TransmissionRequestBuilder requestBuilder = oxalisOutboundModule.getTransmissionRequestBuilder();

		ByteArrayOutputStream os = new ByteArrayOutputStream();

        marshal(sbd, os);

		requestBuilder.payLoad(new ByteArrayInputStream(os.toByteArray()));
		TransmissionRequest transmissionRequest = requestBuilder.build();

		Transmitter transmitter = oxalisOutboundModule.getTransmitter();
		transmitter.transmit(transmissionRequest);

	}

	private List<Scope> createOxalisSpecificScopes() {
		List<Scope> scopes = new ArrayList<Scope>();
		scopes.add(createScope("DOCUMENTID", null,
				"urn:no:difi:meldingsuveksling:xsd::Melding##urn:www.difi.no:meldingsutveksling:melding:1.0:extended:urn:www.difi.no:encoded:aes-zip:1.0::1.0"));
		scopes.add(createScope("PROCESSID", null, "urn:www.difi.no:profile:meldingsutveksling:ver1.0"));
		return scopes;
	}

	private Scope createScope(String type, String identifier, String instanceIdentifier) {
		Scope scope = new Scope();

		scope.setIdentifier(identifier);
		scope.setInstanceIdentifier(instanceIdentifier);
		scope.setType(type);

		return scope;
	}

}
