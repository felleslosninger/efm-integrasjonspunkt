package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.BestEduMessage;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;


/**
 * Factory class for StandardBusinessDocument instances
 */
@Component
public class StandardBusinessDocumentFactory {

    @Autowired
    private IntegrasjonspunktNokkel integrasjonspunktNokkel;

    public StandardBusinessDocumentFactory() {
    }

    public StandardBusinessDocumentFactory(IntegrasjonspunktNokkel integrasjonspunktNokkel) {
        this.integrasjonspunktNokkel = integrasjonspunktNokkel;
    }

    public StandardBusinessDocument create(PutMessageRequestType sender, Avsender avsender, Mottaker mottaker) throws IOException {
        return create(sender, UUID.randomUUID().toString(), avsender, mottaker);
    }

    public StandardBusinessDocument create(PutMessageRequestType shipment, String id, Avsender avsender, Mottaker mottaker) throws IOException {
        final byte[] marshalledShipment = marshall(shipment);

        BestEduMessage bestEduMessage = new BestEduMessage(marshalledShipment);
        Archive archive = createAsicePackage(avsender, mottaker, bestEduMessage);
        Payload payload = new Payload(encryptArchive(mottaker, archive));

        return new CreateSBD().createSBD(avsender.getOrgNummer(), mottaker.getOrgNummer(), payload, id, "melding");
    }

    private byte[] encryptArchive(Mottaker mottaker, Archive archive) {
        return new CmsUtil().createCMS(archive.getBytes()
                , mottaker.getSertifikat());
    }

    private Archive createAsicePackage(Avsender avsender, Mottaker mottaker, BestEduMessage bestEduMessage) throws IOException {
        return new CreateAsice().createAsice(bestEduMessage, integrasjonspunktNokkel.getSignatureHelper(), avsender, mottaker);
    }

    private byte[] marshall(PutMessageRequestType sender) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(new ObjectFactory().createPutMessageRequest(sender), os);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return os.toByteArray();
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