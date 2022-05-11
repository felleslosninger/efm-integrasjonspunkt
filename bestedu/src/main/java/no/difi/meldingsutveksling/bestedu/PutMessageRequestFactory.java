package no.difi.meldingsutveksling.bestedu;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.core.Receiver;
import no.difi.meldingsutveksling.core.Sender;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PutMessageRequestFactory {

    private final ServiceRegistryLookup srLookup;

    public PutMessageRequestType create(StandardBusinessDocument sbd, Object payload) {
        return create(sbd, payload, sbd.getConversationId());
    }

    public PutMessageRequestType create(StandardBusinessDocument sbd, Object payload, String conversationId) {
        String senderRef = SBDUtil.getOptionalSenderRef(sbd).orElse(null);
        String receiverRef = SBDUtil.getOptionalReceiverRef(sbd).orElse(null);
        InfoRecord receiverInfo = srLookup.getInfoRecord(sbd.getReceiverIdentifier().getPrimaryIdentifier());
        InfoRecord senderInfo = srLookup.getInfoRecord(sbd.getSenderIdentifier().getPrimaryIdentifier());
        return create(conversationId,
                Sender.of(sbd.getSenderIdentifier().getPrimaryIdentifier(), senderInfo.getOrganizationName(), senderRef),
                Receiver.of(sbd.getReceiverIdentifier().getPrimaryIdentifier(), receiverInfo.getOrganizationName(), receiverRef),
                payload);
    }

    public PutMessageRequestType createAndSwitchSenderReceiver(StandardBusinessDocument sbd, Object payload, String conversationId) {
        String senderRef = SBDUtil.getOptionalSenderRef(sbd).orElse(null);
        String receiverRef = SBDUtil.getOptionalReceiverRef(sbd).orElse(null);
        InfoRecord senderInfo = srLookup.getInfoRecord(sbd.getReceiverIdentifier().getPrimaryIdentifier());
        InfoRecord receiverInfo = srLookup.getInfoRecord(sbd.getSenderIdentifier().getPrimaryIdentifier());
        return create(conversationId,
                Sender.of(sbd.getReceiverIdentifier().getPrimaryIdentifier(), senderInfo.getOrganizationName(), senderRef),
                Receiver.of(sbd.getSenderIdentifier().getPrimaryIdentifier(), receiverInfo.getOrganizationName(), receiverRef),
                payload);
    }

    public PutMessageRequestType create(String conversationId,
                                        Sender sender,
                                        Receiver receiver,
                                        Object payload) {

        no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory of = new no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory();

        AddressType receiverAddressType = of.createAddressType();
        receiverAddressType.setOrgnr(receiver.getIdentifier());
        receiverAddressType.setName(receiver.getName());
        receiverAddressType.setRef(receiver.getRef());

        AddressType senderAddressType = of.createAddressType();
        senderAddressType.setOrgnr(sender.getIdentifier());
        senderAddressType.setName(sender.getName());
        senderAddressType.setRef(sender.getRef());

        EnvelopeType envelopeType = of.createEnvelopeType();
        envelopeType.setConversationId(conversationId);
        envelopeType.setContentNamespace("http://www.arkivverket.no/Noark4-1-WS-WD/types");
        envelopeType.setReceiver(receiverAddressType);
        envelopeType.setSender(senderAddressType);

        PutMessageRequestType putMessageRequestType = of.createPutMessageRequestType();
        putMessageRequestType.setEnvelope(envelopeType);
        putMessageRequestType.setPayload(payload);

        return putMessageRequestType;
    }
}
