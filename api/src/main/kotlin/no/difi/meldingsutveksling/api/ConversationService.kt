package no.difi.meldingsutveksling.api

import no.difi.meldingsutveksling.MessageInformable
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.nextmove.ConversationDirection
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import no.difi.meldingsutveksling.status.Conversation
import no.difi.meldingsutveksling.status.MessageStatus
import java.util.*

interface ConversationService {
    fun registerStatus(messageId: String, status: MessageStatus): Optional<Conversation>
    fun registerStatus(messageId: String, vararg status: ReceiptStatus): Optional<Conversation>
    fun registerStatus(messageId: String, status: ReceiptStatus, description: String): Optional<Conversation>
    fun registerStatus(conversation: Conversation, status: MessageStatus): Conversation
    fun save(conversation: Conversation): Conversation
    fun registerConversation(message: MessageInformable, vararg statuses: ReceiptStatus): Conversation
    fun registerConversation(sbd: StandardBusinessDocument, si: ServiceIdentifier, direction: ConversationDirection, vararg statuses: ReceiptStatus): Conversation
    fun findConversation(messageId: String): Optional<Conversation>
    fun findConversation(conversationId: String, direction: ConversationDirection): Optional<Conversation>
}