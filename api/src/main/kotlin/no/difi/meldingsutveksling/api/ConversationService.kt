package no.difi.meldingsutveksling.api

import no.difi.meldingsutveksling.MessageInformable
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.nextmove.ConversationDirection
import no.difi.meldingsutveksling.status.Conversation
import no.difi.meldingsutveksling.status.MessageStatus
import java.util.*

interface ConversationService {
    fun registerStatus(messageId: String, status: MessageStatus): Optional<Conversation>
    fun registerStatus(conversation: Conversation, status: MessageStatus): Conversation
    fun save(conversation: Conversation): Conversation
    fun registerConversation(message: MessageInformable): Conversation
    fun registerConversation(sbd: StandardBusinessDocument, si: ServiceIdentifier, direction: ConversationDirection): Conversation
    fun findConversation(messageId: String): Optional<Conversation>
}