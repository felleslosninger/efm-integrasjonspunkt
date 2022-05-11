package no.difi.meldingsutveksling.api

import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.status.Conversation
import no.difi.meldingsutveksling.status.MessageStatus

interface StatusStrategy {
    fun checkStatus(conversations: MutableSet<Conversation>)
    fun getServiceIdentifier(): ServiceIdentifier
    fun isStartPolling(status: MessageStatus): Boolean
    fun isStopPolling(status: MessageStatus): Boolean
}