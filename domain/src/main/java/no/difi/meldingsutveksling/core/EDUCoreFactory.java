package no.difi.meldingsutveksling.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.MeldingFactory;
import no.difi.meldingsutveksling.noarkexchange.PayloadException;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPF;

@Slf4j
@Component
@RequiredArgsConstructor
public class EDUCoreFactory {

    private final ServiceRegistryLookup serviceRegistryLookup;

    public EDUCore create(PutMessageRequestType putMessageRequestType, String senderOrgNr) {
        PutMessageRequestWrapper requestWrapper = new PutMessageRequestWrapper(putMessageRequestType);
        EDUCore eduCore = createCommon(senderOrgNr, requestWrapper.getReceiverPartyNumber(),
                requestWrapper.getEnvelope().getSender().getRef(), requestWrapper.getEnvelope().getReceiver().getRef());

        eduCore.setPayload(putMessageRequestType.getPayload());
        eduCore.setId(requestWrapper.getConversationId());
        if (PayloadUtil.isAppReceipt(putMessageRequestType.getPayload())) {
            eduCore.setMessageType(EDUCore.MessageType.APPRECEIPT);
        } else {
            eduCore.setMessageType(EDUCore.MessageType.EDU);
            String saIdXpath = "Melding/noarksak/saId";
            String jpostnrXpath = "Melding/journpost/jpJpostnr";
            String saId = "";
            String jpJpostnr = "";
            try {
                saId = PayloadUtil.queryPayload(eduCore.getPayload(), saIdXpath);
                jpJpostnr = PayloadUtil.queryPayload(eduCore.getPayload(), jpostnrXpath);
            } catch (PayloadException e) {
                log.error("Failed reading saId and/or jpJpostnr from payload", e);
            }

            if (!isNullOrEmpty(saId) && !isNullOrEmpty(jpJpostnr)) {
                eduCore.setMessageReference(String.format("%s-%s", saId, jpJpostnr));
            }
        }

        return eduCore;
    }

    public EDUCore create(AppReceiptType appReceiptType, String conversationId, String senderOrgnr, String receiverOrgnr) {
        EDUCore eduCore = createCommon(senderOrgnr, receiverOrgnr, null, null);
        eduCore.setId(conversationId);
        eduCore.setPayload(EDUCoreConverter.appReceiptAsString(appReceiptType));
        eduCore.setMessageType(EDUCore.MessageType.APPRECEIPT);
        return eduCore;
    }

    public static PutMessageRequestType createPutMessageFromCore(EDUCore message) {
        no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory of = new no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory();

        AddressType receiverAddressType = of.createAddressType();
        receiverAddressType.setOrgnr(message.getReceiver().getIdentifier());
        receiverAddressType.setName(message.getReceiver().getName());
        receiverAddressType.setRef(message.getReceiver().getRef());

        AddressType senderAddressType = of.createAddressType();
        senderAddressType.setOrgnr(message.getSender().getIdentifier());
        senderAddressType.setName(message.getSender().getName());
        senderAddressType.setRef(message.getSender().getRef());

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

    public EDUCore create(StandardBusinessDocument sbd, Arkivmelding am, byte[] asic) {
        String senderRef = sbd.findScope(ScopeType.SENDER_REF).map(Scope::getIdentifier).orElse(null);
        String receiverRef = sbd.findScope(ScopeType.RECEIVER_REF).map(Scope::getIdentifier).orElse(null);
        EDUCore eduCore = createCommon(sbd.getSenderIdentifier(), sbd.getReceiverIdentifier(), senderRef, receiverRef);
        eduCore.setId(sbd.getConversationId());
        eduCore.setMessageType(EDUCore.MessageType.EDU);
        eduCore.setMessageReference(sbd.getConversationId());

        MeldingType meldingType = MeldingFactory.create(am, asic);
        eduCore.setPayload(EDUCoreConverter.meldingTypeAsString(meldingType));

        return eduCore;
    }

    private EDUCore createCommon(String senderOrgNr, String receiverOrgNr, String senderRef, String receiverRef) {

        InfoRecord senderInfo = serviceRegistryLookup.getInfoRecord(senderOrgNr);
        InfoRecord receiverInfo = serviceRegistryLookup.getInfoRecord(receiverOrgNr);

        EDUCore eduCore = new EDUCore();
        ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(receiverOrgNr);
        } catch (ServiceRegistryLookupException e) {
            log.error("Error looking up service record for {}", receiverOrgNr, e);
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        eduCore.setServiceIdentifier(serviceRecord.getServiceIdentifier());

        eduCore.setSender(Sender.of(senderInfo.getIdentifier(), senderInfo.getOrganizationName(), serviceRecord.getServiceIdentifier() == DPF ? senderRef : null));
        eduCore.setReceiver(Receiver.of(receiverInfo.getIdentifier(), receiverInfo.getOrganizationName(), serviceRecord.getServiceIdentifier() == DPF ? receiverRef : null));


        return eduCore;
    }

}
