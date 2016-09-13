package no.difi.meldingsutveksling.core;

import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class EDUCoreFactory {

    private ServiceRegistryLookup serviceRegistryLookup;

    public EDUCoreFactory(ServiceRegistryLookup serviceRegistryLookup) {
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    public EDUMessage createEduMessage(PutMessageRequestType putMessageRequestType, String senderOrgNr) {
        return (EDUMessage) create(putMessageRequestType, senderOrgNr);
    }

    public EDUAppReceipt createEduAppReceipt(PutMessageRequestType putMessageRequestType, String senderOrgNr) {
        return (EDUAppReceipt) create(putMessageRequestType, senderOrgNr);
    }

    public EDUCore create(PutMessageRequestType putMessageRequestType, String senderOrgNr) {
        EDUCore eduCore = createCommon(senderOrgNr, putMessageRequestType.getEnvelope().getReceiver().getOrgnr());

        eduCore.setId(putMessageRequestType.getEnvelope().getConversationId());
        if (PayloadUtil.isAppReceipt(putMessageRequestType.getPayload())) {
            eduCore.setMessageType(EDUCore.MessageType.APPRECEIPT);
        } else {
            eduCore.setMessageType(EDUCore.MessageType.EDU);
        }
        eduCore.setPayload(unmarshallPayload(putMessageRequestType.getPayload()));

        return eduCore;
    }

    public EDUMessage createEduMessage(Message message, String senderOrgNr) {
        return (EDUMessage) create(message, senderOrgNr);
    }

    public EDUCore create(Message message, String senderOrgNr) {
        EDUCore eduCore = createCommon(senderOrgNr, message.getParticipantId());

        eduCore.setId(message.getMessageReference());
        eduCore.setMessageType(EDUCore.MessageType.MXA);

        ObjectFactory of = new ObjectFactory();

        JournpostType journpostType = of.createJournpostType();
        journpostType.setJpInnhold(message.getContent().getMessageHeader());
        journpostType.setJpOffinnhold(message.getContent().getMessageSummery());
        journpostType.setJpId(message.getIdproc());

        message.getContent().getAttachments().getAttachment().forEach(a -> {
            DokumentType dokumentType = of.createDokumentType();
            dokumentType.setVeFilnavn(a.getFilename());
            dokumentType.setVeMimeType(a.getMimeType());
            FilType filType = of.createFilType();
            filType.setBase64(a.getValue().getBytes());
            dokumentType.setFil(filType);

            journpostType.getDokument().add(dokumentType);
        });

        MeldingType meldingType = of.createMeldingType();
        meldingType.setJournpost(journpostType);

        eduCore.setPayload(meldingType);

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

    private void fillCommon(EDUCore core, String senderOrgNr, String receiverOrgNr) {
        InfoRecord senderInfo = serviceRegistryLookup.getInfoRecord(senderOrgNr);
        InfoRecord receiverInfo = serviceRegistryLookup.getInfoRecord(receiverOrgNr);

        core.setSender(createSender(senderInfo));
        core.setReceiver(createReceiver(receiverInfo));
    }

    public Object unmarshallPayload(Object payload) {
        String p;
        Object msg = null;

        if (payload instanceof String) {
            p = (String) payload;
            p = StringEscapeUtils.unescapeHtml(p);
        } else {
            p = ((Node) payload).getFirstChild().getTextContent().trim();
        }

        try {
            if (PayloadUtil.isAppReceipt(payload)) {
                JAXBContext jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema");
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                msg = unmarshaller.unmarshal(new StringSource((p)), AppReceiptType.class).getValue();
            } else {
                JAXBContext jaxbContext = JAXBContext.newInstance(MeldingType.class);
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                msg = unmarshaller.unmarshal(new StringSource((p)), MeldingType.class).getValue();
            }
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
