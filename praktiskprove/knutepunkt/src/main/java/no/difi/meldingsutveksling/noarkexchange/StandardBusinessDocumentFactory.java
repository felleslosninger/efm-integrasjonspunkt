package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.BestEduMessage;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;

import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class StandardBusinessDocumentFactory {

    static StandardBusinessDocument create(PutMessageRequestType sender, Avsender avsender, Mottaker mottaker) {
        Dokumentpakker pakker = new Dokumentpakker();

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(new ObjectFactory().createPutMessageRequest(sender), os);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return pakker.pakk(new BestEduMessage(os.toByteArray()), avsender, mottaker, UUID.randomUUID().toString(), "melding");
    }

    static StandardBusinessDocument create(no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument fromDocument) {
        try {
            JAXBContext jaxbContextFrom = JAXBContext.newInstance(no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class);
            JAXBContext jaxbContextTo = JAXBContext.newInstance(StandardBusinessDocument.class);

            Marshaller jaxbMarshaller = jaxbContextTo.createMarshaller();
            Unmarshaller unMarshaller = jaxbContextFrom.createUnmarshaller();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            jaxbMarshaller.marshal(fromDocument, os);

            JAXBElement<StandardBusinessDocument> result = unMarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(os.toByteArray())), StandardBusinessDocument.class);
            return result.getValue();

        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
    }

}