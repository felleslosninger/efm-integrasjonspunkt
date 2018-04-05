package no.difi.meldingsutveksling.nextmove.validation;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.nextmove.ConversationResource;
import no.difi.meldingsutveksling.nextmove.NextMoveException;

public interface ConversationValidator {
    void validate(ConversationResource cr) throws NextMoveException;
    ServiceIdentifier getServicIdentifier();
}
