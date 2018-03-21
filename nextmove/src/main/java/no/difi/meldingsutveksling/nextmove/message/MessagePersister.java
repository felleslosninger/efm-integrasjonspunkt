package no.difi.meldingsutveksling.nextmove.message;

import no.difi.meldingsutveksling.nextmove.ConversationResource;

import java.io.IOException;

public interface MessagePersister {

    void write(ConversationResource cr, String filename, byte[] message) throws IOException;

    byte[] read(ConversationResource cr, String filename) throws IOException;

    void delete(ConversationResource cr) throws IOException;
}
