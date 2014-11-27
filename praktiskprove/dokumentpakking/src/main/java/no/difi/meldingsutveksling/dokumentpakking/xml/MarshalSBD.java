package no.difi.meldingsutveksling.dokumentpakking.xml;

import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;


public final class MarshalSBD {
	
	private MarshalSBD() {
	}
	public static void marshal(StandardBusinessDocument doc, OutputStream os) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(StandardBusinessDocument.class, Payload.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(new ObjectFactory().createStandardBusinessDocument(doc), os);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
}
