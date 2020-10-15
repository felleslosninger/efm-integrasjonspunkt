package no.difi.meldingsutveksling.api

import no.difi.meldingsutveksling.nextmove.NextMoveException
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage

interface ConversationStrategy {
    @Throws(NextMoveException::class)
    fun send(message: NextMoveOutMessage)
}

interface DpvConversationStrategy: ConversationStrategy {}

interface DpoConversationStrategy: ConversationStrategy {}

interface DpeConversationStrategy: ConversationStrategy {}

interface DpfConversationStrategy: ConversationStrategy {}

interface DpiConversationStrategy: ConversationStrategy {}

