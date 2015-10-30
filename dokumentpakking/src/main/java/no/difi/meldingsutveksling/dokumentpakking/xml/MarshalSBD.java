package no.difi.meldingsutveksling.dokumentpakking.xml;

import no.difi.meldingsutveksling.dokumentpakking.kvit.Kvittering;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.OutputStream;


public final class MarshalSBD {
	
	private MarshalSBD() {
	}
	public static void marshal(Document doc, OutputStream os) {
		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(new Class[] {Document.class, Payload.class, Kvittering.class});
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(new ObjectFactory().createStandardBusinessDocument(doc), os);
		} catch (JAXBException e) {
			throw new MeldingsUtvekslingRuntimeException("Was not able to marshall Standard Business Document", e);
		}
	}
}
