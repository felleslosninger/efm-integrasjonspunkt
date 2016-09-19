package no.difi.meldingsutveksling.core;

import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
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

    public PutMessageRequestType createPutMessageFromCore(EDUCore message) {
        no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory of = new no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory();

        AddressType receiverAddressType = of.createAddressType();
        receiverAddressType.setOrgnr(message.getReceiver().getOrgNr());
        receiverAddressType.setName(message.getReceiver().getOrgName());

        AddressType senderAddressType = of.createAddressType();
        senderAddressType.setOrgnr(message.getSender().getOrgNr());
        senderAddressType.setName(message.getSender().getOrgName());

        EnvelopeType envelopeType = of.createEnvelopeType();
        envelopeType.setConversationId(message.getId());
        envelopeType.setReceiver(receiverAddressType);
        envelopeType.setSender(senderAddressType);

        PutMessageRequestType putMessageRequestType = of.createPutMessageRequestType();
        putMessageRequestType.setEnvelope(envelopeType);
        putMessageRequestType.setPayload(message.getPayload());

        // TODO: add potential missing fields: ref, email?

        return putMessageRequestType;
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

        // TODO: add sender/receiver orgnr validation

        InfoRecord senderInfo = serviceRegistryLookup.getInfoRecord(senderOrgNr);
        InfoRecord receiverInfo = serviceRegistryLookup.getInfoRecord(receiverOrgNr);

        // TODO: verify info objects

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

        try { // TODO: see AppReceiptPutMessageStrategy, error handling
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
