package no.difi.meldingsutveksling.core;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.PutMessageMarker;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
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
import java.util.Base64;

public class EDUCoreFactory {

    private ServiceRegistryLookup serviceRegistryLookup;

    public EDUCoreFactory(ServiceRegistryLookup serviceRegistryLookup) {
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    public EDUCore create(PutMessageRequestType putMessageRequestType, String senderOrgNr) {
        PutMessageRequestWrapper requestWrapper = new PutMessageRequestWrapper(putMessageRequestType);
        EDUCore eduCore = createCommon(senderOrgNr, requestWrapper.getRecieverPartyNumber());

        try {
            eduCore.setPayload(unmarshallPayload(putMessageRequestType.getPayload()));
        } catch (JAXBException e) {
            Audit.error("Payload unmarshalling failed. Request causing error: {}",
                    PutMessageMarker.markerFrom(new PutMessageRequestWrapper(putMessageRequestType)));
            throw new MeldingsUtvekslingRuntimeException(e);
        }

        eduCore.setId(requestWrapper.getConversationId());
        if (PayloadUtil.isAppReceipt(putMessageRequestType.getPayload())) {
            eduCore.setMessageType(EDUCore.MessageType.APPRECEIPT);
        } else {
            eduCore.setMessageType(EDUCore.MessageType.EDU);
            eduCore.setMessageReference(String.format("%s-%s",
                    eduCore.getPayloadAsMeldingType().getNoarksak().getSaId(),
                    eduCore.getPayloadAsMeldingType().getJournpost().getJpJpostnr()));
        }


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

        return putMessageRequestType;
    }

    public EDUCore create(Message message, String senderOrgNr) {
        EDUCore eduCore = createCommon(senderOrgNr, message.getParticipantId());

        eduCore.setId(message.getIdproc());
        eduCore.setMessageReference(message.getMessageReference());
        eduCore.setMessageType(EDUCore.MessageType.EDU);

        ObjectFactory of = new ObjectFactory();

        JournpostType journpostType = of.createJournpostType();
        journpostType.setJpInnhold(message.getContent().getMessageHeader());
        journpostType.setJpOffinnhold(message.getContent().getMessageSummery());
        journpostType.setJpId(message.getMessageReference());

        message.getContent().getAttachments().getAttachment().forEach(a -> {
            DokumentType dokumentType = of.createDokumentType();
            dokumentType.setVeFilnavn(a.getFilename());
            dokumentType.setVeMimeType(a.getMimeType());
            FilType filType = of.createFilType();
            filType.setBase64(Base64.getDecoder().decode(a.getValue()));
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

    public Object unmarshallPayload(Object payload) throws JAXBException {
        String p;
        Object msg = null;

        if (payload instanceof String) {
            p = (String) payload;
            p = StringEscapeUtils.unescapeHtml(p);
        } else {
            p = ((Node) payload).getFirstChild().getTextContent().trim();
        }

        if (PayloadUtil.isAppReceipt(payload)) {
            JAXBContext jaxbContext = JAXBContext.newInstance("no.difi.meldingsutveksling.noarkexchange.schema");
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            msg = unmarshaller.unmarshal(new StringSource((p)), AppReceiptType.class).getValue();
        } else {
            JAXBContext jaxbContext = JAXBContext.newInstance(MeldingType.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            msg = unmarshaller.unmarshal(new StringSource((p)), MeldingType.class).getValue();
        }
        return msg;
    }

    private Sender createSender(InfoRecord senderInfo) {
        Sender sender = new Sender();
        sender.setOrgNr(senderInfo.getIdentifier());
        sender.setOrgName(senderInfo.getOrganizationName());
        return sender;
    }

    private Receiver createReceiver(InfoRecord receiverInfo) {
        Receiver receiver = new Receiver();
        receiver.setOrgNr(receiverInfo.getIdentifier());
        receiver.setOrgName(receiverInfo.getOrganizationName());
        return receiver;
    }

}
