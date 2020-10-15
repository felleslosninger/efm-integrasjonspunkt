package no.difi.meldingsutveksling.api

import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.status.Conversation

interface StatusStrategy {
    fun checkStatus(conversations: MutableSet<Conversation>)
    fun getServiceIdentifier(): ServiceIdentifier
}