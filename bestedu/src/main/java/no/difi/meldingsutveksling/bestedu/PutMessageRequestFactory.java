package no.difi.meldingsutveksling.bestedu;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.core.Receiver;
import no.difi.meldingsutveksling.core.Sender;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
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
    private final SBDService sbdService;

    public PutMessageRequestType create(StandardBusinessDocument sbd, Object payload) {
        return create(sbd, payload, SBDUtil.getConversationId(sbd));
    }

    public PutMessageRequestType create(StandardBusinessDocument sbd, Object payload, String conversationId) {
        String senderRef = SBDUtil.getOptionalSenderRef(sbd).orElse(null);
        String receiverRef = SBDUtil.getOptionalReceiverRef(sbd).orElse(null);
        InfoRecord receiverInfo = srLookup.getInfoRecord(sbdService.getReceiverIdentifier(sbd));
        InfoRecord senderInfo = srLookup.getInfoRecord(sbdService.getSenderIdentifier(sbd));
        return create(conversationId,
                Sender.of(sbdService.getSenderIdentifier(sbd), senderInfo.getOrganizationName(), senderRef),
                Receiver.of(sbdService.getReceiverIdentifier(sbd), receiverInfo.getOrganizationName(), receiverRef),
                payload);
    }

    public PutMessageRequestType createAndSwitchSenderReceiver(StandardBusinessDocument sbd, Object payload, String conversationId) {
        String senderRef = SBDUtil.getOptionalSenderRef(sbd).orElse(null);
        String receiverRef = SBDUtil.getOptionalReceiverRef(sbd).orElse(null);
        InfoRecord senderInfo = srLookup.getInfoRecord(sbdService.getReceiverIdentifier(sbd));
        InfoRecord receiverInfo = srLookup.getInfoRecord(sbdService.getSenderIdentifier(sbd));
        return create(conversationId,
                Sender.of(sbdService.getReceiverIdentifier(sbd), senderInfo.getOrganizationName(), senderRef),
                Receiver.of(sbdService.getSenderIdentifier(sbd), receiverInfo.getOrganizationName(), receiverRef),
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
