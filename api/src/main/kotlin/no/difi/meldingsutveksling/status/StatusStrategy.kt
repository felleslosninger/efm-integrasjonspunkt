package no.difi.meldingsutveksling.status

import no.difi.meldingsutveksling.ServiceIdentifier

interface StatusStrategy {
    fun checkStatus(conversation: Conversation)
    fun getServiceIdentifier(): ServiceIdentifier
}