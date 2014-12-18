package no.difi.meldingsutveksling.noarkexchange;

import eu.peppol.outbound.OxalisOutboundModule;
import eu.peppol.outbound.transmission.TransmissionRequest;
import eu.peppol.outbound.transmission.TransmissionRequestBuilder;
import eu.peppol.outbound.transmission.Transmitter;
import no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("dev")
public class OxalisSendMessageTemplate extends SendMessageTemplate {

	@Override
	void sendSBD(StandardBusinessDocument sbd) throws IOException {

		sbd.getStandardBusinessDocumentHeader().getBusinessScope().getScope().addAll(createOxalisSpecificScopes());

		OxalisOutboundModule oxalisOutboundModule = new OxalisOutboundModule();

		TransmissionRequestBuilder requestBuilder = oxalisOutboundModule.getTransmissionRequestBuilder();

		ByteArrayOutputStream os = new ByteArrayOutputStream();
   /*     //allerede marshaled # new ObjectFactory().createStandardBusinessDocument(doc) legger inn ny verdi?
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer  ;
        try {
            transformer = transformerFactory.newTransformer();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(sbd);
            oos.flush();
            oos.close();
            InputStream is = new ByteArrayInputStream(baos.toByteArray());
            transformer.transform(new StreamSource(is), new StreamResult(outputStream));
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            e.printStackTrace();
        }*/
        MarshalSBD.marshal(sbd, os);


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
