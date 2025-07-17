package no.difi.meldingsutveksling.receipt.strategy

import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.ServiceIdentifier.DPV
import no.difi.meldingsutveksling.altinnv3.DPV.AltinnDPVService
import no.difi.meldingsutveksling.api.ConversationService
import no.difi.meldingsutveksling.api.NextMoveQueue
import no.difi.meldingsutveksling.api.StatusStrategy
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringType.OK
import no.difi.meldingsutveksling.receipt.ReceiptStatus.*
import no.difi.meldingsutveksling.sbd.SBDFactory
import no.difi.meldingsutveksling.status.Conversation
import no.difi.meldingsutveksling.status.ConversationMarker.markerFrom
import no.difi.meldingsutveksling.status.MessageStatus
import no.difi.meldingsutveksling.status.MessageStatusFactory
import no.digdir.altinn3.correspondence.model.CorrespondenceStatusEventExt
import no.digdir.altinn3.correspondence.model.CorrespondenceStatusExt
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["difi.move.feature.enableDPV"], havingValue = "true")
class DpvStatusStrategy(private val conversationService: ConversationService,
                        private val messageStatusFactory: MessageStatusFactory,
                        private val sbdFactory: SBDFactory,
                        private val properties: IntegrasjonspunktProperties,
                        private val nextMoveQueue: NextMoveQueue,
                        private val altinnService: AltinnDPVService) : StatusStrategy {

    val log = LoggerFactory.getLogger(DpvStatusStrategy::class.java)

    override fun checkStatus(conversations: MutableSet<Conversation>) {
        log.debug("Checking status for ${conversations.size} DPV messages..")

        conversations.forEach { conversation ->
            try {
                val statues = altinnService.getStatus(conversation)
                updateStatus(conversation, statues)
            } catch (e: Exception) {
                log.error("Error during status check for " + conversation.conversationId, e)
            }
        }
    }

    private fun updateStatus(c: Conversation, status: List<CorrespondenceStatusEventExt>) {
        log.debug(markerFrom(c), "Checking status for message [id=${c.messageId}, conversationId=${c.conversationId}]")
        status.forEach {
            val mappedStatus = when (it.status) {
                CorrespondenceStatusExt.PUBLISHED -> LEVERT
                CorrespondenceStatusExt.READ -> LEST
                else -> ANNET
            }

            val ms = messageStatusFactory.getMessageStatus(mappedStatus)
            if (!c.hasStatus(ms)) {
                if (mappedStatus == LEVERT &&
                    c.documentIdentifier == properties.arkivmelding.defaultDocumentType &&
                    properties.arkivmelding.isGenerateReceipts) {
                    nextMoveQueue.enqueueIncomingMessage(sbdFactory.createArkivmeldingReceiptFrom(c, OK), DPV)
                }
                conversationService.registerStatus(c, ms)
            }
        }
    }

    override fun getServiceIdentifier(): ServiceIdentifier {
        return DPV
    }

    override fun isStartPolling(status: MessageStatus): Boolean {
        return SENDT.toString() == status.status
    }

    override fun isStopPolling(status: MessageStatus): Boolean {
        return false
    }


}
