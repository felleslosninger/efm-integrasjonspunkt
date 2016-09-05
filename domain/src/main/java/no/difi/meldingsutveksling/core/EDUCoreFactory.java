package no.difi.meldingsutveksling.core;

import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class EDUCoreFactory {

    private ServiceRegistryLookup serviceRegistryLookup;

    public EDUCoreFactory(ServiceRegistryLookup serviceRegistryLookup) {
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    public EDUCore create(PutMessageRequestType putMessageRequestType, String senderOrgNr) {
        EDUCore eduCore = createCommon(senderOrgNr, putMessageRequestType.getEnvelope().getReceiver().getOrgnr());

        eduCore.setId(putMessageRequestType.getEnvelope().getConversationId());
        eduCore.setMeldingType(unmarshallPayload((String)putMessageRequestType.getPayload()));

        return eduCore;
    }

    public EDUCore create(Message message, String senderOrgNr) {
        EDUCore eduCore = createCommon(senderOrgNr, message.getParticipantId());

        eduCore.setId(message.getMessageReference());

        ObjectFactory of = new ObjectFactory();

        JournpostType journpostType = of.createJournpostType();
        journpostType.setJpInnhold(message.getContent().getMessageHeader());
        journpostType.setJpOffinnhold(message.getContent().getMessageSummery());

        message.getContent().getAttachments().getAttachment().forEach(a -> {
            DokumentType dokumentType = of.createDokumentType();
            dokumentType.setVeFilnavn(a.getFilename());
            dokumentType.setVeVariant(a.getName());
            dokumentType.setVeMimeType(a.getMimeType());
            FilType filType = of.createFilType();
            filType.setBase64(a.getValue().getBytes());
            dokumentType.setFil(filType);

            journpostType.getDokument().add(dokumentType);
        });

        MeldingType meldingType = of.createMeldingType();
        meldingType.setJournpost(journpostType);

        return eduCore;
    }

    private EDUCore createCommon(String senderOrgNr, String receiverOrgNr) {

        InfoRecord senderInfo = serviceRegistryLookup.getInfoRecord(senderOrgNr);
        InfoRecord receiverInfo = serviceRegistryLookup.getInfoRecord(receiverOrgNr);

        EDUCore eduCore = new EDUCore();

        eduCore.setSender(createSender(senderInfo));
        eduCore.setReceiver(createReceiver(receiverInfo));

        return eduCore;
    }

    public MeldingType unmarshallPayload(String payload) {
        MeldingType msg = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(MeldingType.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            msg = unmarshaller.unmarshal(new StringSource((payload)), MeldingType.class).getValue();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return msg;
    }

    private Sender createSender(InfoRecord senderInfo) {
        Sender sender = new Sender();
        sender.setOrgNr(senderInfo.getOrganisationNumber());
        sender.setOrgName(senderInfo.getOrganizationName());
        return sender;
    }

    private Receiver createReceiver(InfoRecord receiverInfo) {
        Receiver receiver = new Receiver();
        receiver.setOrgNr(receiverInfo.getOrganisationNumber());
        receiver.setOrgName(receiverInfo.getOrganizationName());
        return receiver;
    }
}
