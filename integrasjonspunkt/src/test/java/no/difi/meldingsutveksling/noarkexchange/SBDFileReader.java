package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

public class SBDFileReader {

    public static EduDocument readSBD(String filename) throws JAXBException {
        JAXBElement<StandardBusinessDocument> fromDocument;
        JAXBContext ctx = JAXBContext.newInstance(StandardBusinessDocument.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        fromDocument = unmarshaller.unmarshal(new StreamSource(SBDFileReader.class.getClassLoader().getResourceAsStream(filename)), StandardBusinessDocument.class);
        return StandardBusinessDocumentFactory.create(fromDocument.getValue());
    }
}
