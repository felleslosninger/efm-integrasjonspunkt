package no.difi.meldingsutveksling.nextmove.convert;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.DpoConversationResource;
import org.springframework.stereotype.Component;

@Component
public class DpoArkivmeldingConverter implements ArkivmeldingConverter {

    @Override
    public ConversationResource convert(ConversationResource cr) {
        return DpoConversationResource.of(cr);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPO;
    }
}
