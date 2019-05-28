package no.difi.meldingsutveksling.noarkexchange;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
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
        String receiverRef = sbd.findScope(ScopeType.RECEIVER_REF).map(Scope::getIdentifier).orElse(null);
        String senderRef = sbd.findScope(ScopeType.SENDER_REF).map(Scope::getIdentifier).orElse(null);
        return create(sbd.getConversationId(),
                sbd.getReceiverIdentifier(),
                sbd.getSenderIdentifier(),
                receiverRef,
                senderRef,
                payload);
    }

    private PutMessageRequestType create(String conversationId,
                                         String receiverIdentifier,
                                         String senderIdentifier,
                                         String receiverRef,
                                         String senderRef,
                                         Object payload) {
        InfoRecord receiverInfo = srLookup.getInfoRecord(receiverIdentifier);
        InfoRecord senderInfo = srLookup.getInfoRecord(senderIdentifier);

        no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory of = new no.difi.meldingsutveksling.noarkexchange.schema.ObjectFactory();

        AddressType receiverAddressType = of.createAddressType();
        receiverAddressType.setOrgnr(receiverIdentifier);
        receiverAddressType.setName(receiverInfo.getOrganizationName());
        receiverAddressType.setRef(receiverRef);

        AddressType senderAddressType = of.createAddressType();
        senderAddressType.setOrgnr(senderIdentifier);
        senderAddressType.setName(senderInfo.getOrganizationName());
        senderAddressType.setRef(senderRef);

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
