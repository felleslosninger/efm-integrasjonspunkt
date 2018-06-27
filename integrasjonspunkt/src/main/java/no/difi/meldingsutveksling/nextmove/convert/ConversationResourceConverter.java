package no.difi.meldingsutveksling.nextmove.convert;

import com.google.common.collect.Maps;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.DpiConversationResource;
import no.difi.meldingsutveksling.nextmove.DpvConversationResource;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;

import static java.lang.String.format;

@Component
public class ConversationResourceConverter {

    private ConversationService conversationService;
    private ServiceRegistryLookup srLookup;
    private EnumMap<ServiceIdentifier, ArkivmeldingConverter> dpaConverters = Maps.newEnumMap(ServiceIdentifier.class);

    @Autowired
    public ConversationResourceConverter(ConversationService conversationService,
                                         ServiceRegistryLookup srLookup,
                                         ObjectProvider<List<ArkivmeldingConverter>> converterProvider) {
        this.conversationService = conversationService;
        this.srLookup = srLookup;
        List<ArkivmeldingConverter> converters = converterProvider.getIfAvailable();
        if (converters != null) {
            converters.forEach(c -> dpaConverters.put(c.getServiceIdentifier(), c));
        }
    }

    public ConversationResource convertDpiToDpv(ConversationResource cr) {
        DpvConversationResource dpv = DpvConversationResource.of((DpiConversationResource) cr);
        // Update conversation to make it pollable for receipts
        conversationService.setServiceIdentifier(cr.getConversationId(), ServiceIdentifier.DPV);
        conversationService.setPollable(cr.getConversationId(), true);
        return dpv;
    }

    public ConversationResource convertDpa(ConversationResource cr) throws DpaNotImplementedException {
        ServiceRecord sr = srLookup.getServiceRecord(cr.getReceiverId()).getServiceRecord();
        if (dpaConverters.containsKey(sr.getServiceIdentifier())) {
            return dpaConverters.get(sr.getServiceIdentifier()).convert(cr);
        }
        throw new DpaNotImplementedException(format("DPA converter not implemented for service identifier %s", sr.getServiceIdentifier()));
    }
}
