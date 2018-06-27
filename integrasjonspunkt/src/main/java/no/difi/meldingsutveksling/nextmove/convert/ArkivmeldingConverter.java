package no.difi.meldingsutveksling.nextmove.convert;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationResource;

public interface ArkivmeldingConverter {
    ConversationResource convert(ConversationResource cr);
    ServiceIdentifier getServiceIdentifier();
}
