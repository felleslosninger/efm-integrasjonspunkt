package no.difi.meldingsutveksling.core;

import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.PutMessageMarker;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.*;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;

import javax.xml.bind.JAXBException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static no.difi.meldingsutveksling.noarkexchange.PayloadUtil.unmarshallPayload;

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
                    PutMessageMarker.markerFrom(new PutMessageRequestWrapper(putMessageRequestType)), e);
            throw new MeldingsUtvekslingRuntimeException(e);
        }

        eduCore.setId(requestWrapper.getConversationId());
        if (PayloadUtil.isAppReceipt(putMessageRequestType.getPayload())) {
            eduCore.setMessageType(EDUCore.MessageType.APPRECEIPT);
        } else {
            eduCore.setMessageType(EDUCore.MessageType.EDU);
            Optional<String> saId = Optional.of(eduCore)
                    .map(EDUCore::getPayloadAsMeldingType)
                    .map(MeldingType::getNoarksak)
                    .map(NoarksakType::getSaId);
            Optional<String> jpostnr = Optional.of(eduCore)
                    .map(EDUCore::getPayloadAsMeldingType)
                    .map(MeldingType::getJournpost)
                    .map(JournpostType::getJpJpostnr);
            if (saId.isPresent() && jpostnr.isPresent()) {
                eduCore.setMessageReference(String.format("%s-%s", saId.get(), jpostnr.get()));
            }
        }


        return eduCore;
    }

    public static PutMessageRequestType createPutMessageFromCore(EDUCore message) {
        no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory of = new no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory();

        AddressType receiverAddressType = of.createAddressType();
        receiverAddressType.setOrgnr(message.getReceiver().getIdentifier());
        receiverAddressType.setName(message.getReceiver().getName());

        AddressType senderAddressType = of.createAddressType();
        senderAddressType.setOrgnr(message.getSender().getIdentifier());
        senderAddressType.setName(message.getSender().getName());

        EnvelopeType envelopeType = of.createEnvelopeType();
        envelopeType.setConversationId(message.getId());
        envelopeType.setContentNamespace("http://www.arkivverket.no/Noark4-1-WS-WD/types");
        envelopeType.setReceiver(receiverAddressType);
        envelopeType.setSender(senderAddressType);

        PutMessageRequestType putMessageRequestType = of.createPutMessageRequestType();
        putMessageRequestType.setEnvelope(envelopeType);
        putMessageRequestType.setPayload(message.getPayload());

        return putMessageRequestType;
    }

    public EDUCore create(Message message, String senderOrgNr) {
        EDUCore eduCore = createCommon(senderOrgNr, message.getParticipantId());

        // IdProc is regarded as message id for MXA, but UUID is needed by e.g. DPI.
        String genId = UUID.randomUUID().toString();
        eduCore.setId(genId);
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
            dokumentType.setDbTittel(a.getName());
            FilType filType = of.createFilType();
            filType.setBase64(Base64.getDecoder().decode(a.getValue()));
            dokumentType.setFil(filType);

            journpostType.getDokument().add(dokumentType);
        });

        NoarksakType noarksakType = of.createNoarksakType();
        noarksakType.setSaOfftittel(message.getContent().getMessageHeader());

        MeldingType meldingType = of.createMeldingType();
        meldingType.setJournpost(journpostType);
        meldingType.setNoarksak(noarksakType);

        eduCore.setPayload(meldingType);

        return eduCore;
    }

    public EDUCore create(ConversationResource cr, Arkivmelding arkivmelding) {
        EDUCore eduCore = createCommon(cr.getSenderId(), cr.getReceiverId());
        eduCore.setId(cr.getConversationId());
        eduCore.setMessageType(EDUCore.MessageType.EDU);
        eduCore.setMessageReference(cr.getConversationId());

        // TODO: map arkivmelding to MeldingType
        ObjectFactory of = new ObjectFactory();

        JournpostType journpostType = of.createJournpostType();
        NoarksakType noarksakType = of.createNoarksakType();

        MeldingType meldingType = of.createMeldingType();
        meldingType.setJournpost(journpostType);
        meldingType.setNoarksak(noarksakType);

        // Further usage expects payload to be marshalled


        eduCore.setPayload(meldingType);

        return eduCore;
    }


    private EDUCore createCommon(String senderOrgNr, String receiverOrgNr) {

        InfoRecord senderInfo = serviceRegistryLookup.getInfoRecord(senderOrgNr);
        InfoRecord receiverInfo = serviceRegistryLookup.getInfoRecord(receiverOrgNr);

        EDUCore eduCore = new EDUCore();

        eduCore.setSender(createSender(senderInfo));
        eduCore.setReceiver(createReceiver(receiverInfo));

        ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(receiverOrgNr);
        eduCore.setServiceIdentifier(serviceRecord.getServiceIdentifier());

        return eduCore;
    }

    private Sender createSender(InfoRecord senderInfo) {
        Sender sender = new Sender();
        sender.setIdentifier(senderInfo.getIdentifier());
        sender.setName(senderInfo.getOrganizationName());
        return sender;
    }

    private Receiver createReceiver(InfoRecord receiverInfo) {
        Receiver receiver = new Receiver();
        receiver.setIdentifier(receiverInfo.getIdentifier());
        receiver.setName(receiverInfo.getOrganizationName());
        return receiver;
    }

}
