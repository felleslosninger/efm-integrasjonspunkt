package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.BestEduMessage;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.modelmapper.ModelMapper;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.util.UUID;


/**
 * Factory class for StandardBusinessDocument instances
 */
public class StandardBusinessDocumentFactory {

    static StandardBusinessDocument create(PutMessageRequestType sender, Avsender avsender, Mottaker mottaker) {
        return create(sender, UUID.randomUUID().toString(), avsender, mottaker);
    }

    static StandardBusinessDocument create(PutMessageRequestType sender, String id, Avsender avsender, Mottaker mottaker) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(new ObjectFactory().createPutMessageRequest(sender), os);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return new Dokumentpakker().pakk(new BestEduMessage(os.toByteArray()), avsender, mottaker, UUID.randomUUID().toString(), "melding");
    }

    /**
     * @param fromDocument
     * @return
     */
    static StandardBusinessDocument create(no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument fromDocument) {
        ModelMapper mapper = new ModelMapper();
        return mapper.map(fromDocument, StandardBusinessDocument.class);
    }

    /**
     * @param fromDocument
     * @return
     */
    static no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument create(StandardBusinessDocument fromDocument) {
        ModelMapper mapper = new ModelMapper();
        return mapper.map(fromDocument, no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument.class);
    }


}