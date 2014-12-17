package no.difi.meldingsutveksling.dokumentpakking.xml;

import no.difi.meldingsutveksling.dokumentpakking.kvit.Kvittering;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.OutputStream;


public final class MarshalSBD {
	
	private MarshalSBD() {
	}
	public static void marshal(StandardBusinessDocument doc, OutputStream os) {
		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(new Class[] {StandardBusinessDocument.class, Payload.class, Kvittering.class});
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(new ObjectFactory().createStandardBusinessDocument(doc), os);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
}
