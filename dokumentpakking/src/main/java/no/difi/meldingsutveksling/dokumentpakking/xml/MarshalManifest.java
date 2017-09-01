package no.difi.meldingsutveksling.dokumentpakking.xml;

import org.eclipse.persistence.jaxb.JAXBContextFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.OutputStream;

public final class MarshalManifest {
	private MarshalManifest() {
	}
	public static void marshal(Manifest doc, OutputStream os) {
		try {
			JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{Manifest.class}, null);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(doc, os);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
}
