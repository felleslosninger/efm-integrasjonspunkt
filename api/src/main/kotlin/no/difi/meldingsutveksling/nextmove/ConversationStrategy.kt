package no.difi.meldingsutveksling.nextmove

interface ConversationStrategy {
    @Throws(NextMoveException::class)
    fun send(message: NextMoveOutMessage)
}

interface DpvConversationStrategy: ConversationStrategy {}

interface DpoConversationStrategy: ConversationStrategy {}

interface DpeConversationStrategy: ConversationStrategy {}

interface DpfConversationStrategy: ConversationStrategy {}

interface DpiConversationStrategy: ConversationStrategy {}

