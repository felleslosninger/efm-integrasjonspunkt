package no.difi.meldingsutveksling.nextmove.convert;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.DpfConversationResource;
import org.springframework.stereotype.Component;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPF;

@Component
public class DpfArkivmeldingConverter implements ArkivmeldingConverter {

    @Override
    public ConversationResource convert(ConversationResource cr) {
        return DpfConversationResource.of(cr);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return DPF;
    }
}
