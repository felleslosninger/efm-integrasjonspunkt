package no.difi.meldingsutveksling.dokumentpakking.xml;

import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public final class MarshalManifest {
	private MarshalManifest() {
	}
	public static void marshal(Manifest doc, OutputStream os) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Manifest.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			jaxbMarshaller.marshal(doc, os);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}
}
