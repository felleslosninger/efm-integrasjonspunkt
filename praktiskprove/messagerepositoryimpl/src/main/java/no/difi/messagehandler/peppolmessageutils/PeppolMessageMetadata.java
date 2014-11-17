package no.difi.messagehandler.peppolmessageutils;

import no.difi.meldingsutveksling.eventlog.MessageId;

/**
 * Created with IntelliJ IDEA.
 * User: glennbech
 * Date: 10.11.14
 * Time: 10:27
 * To change this template use File | Settings | File Templates.
 */
public class PeppolMessageMetadata {
    /**
     * The PEPPOL Message Identifier, supplied in the SBDH when using AS2
     */
    private MessageId messageId;


    public MessageId getMessageId() {
        return messageId;
    }
    /** The PEPPOL Participant Identifier of the end point of the receiver */
}
