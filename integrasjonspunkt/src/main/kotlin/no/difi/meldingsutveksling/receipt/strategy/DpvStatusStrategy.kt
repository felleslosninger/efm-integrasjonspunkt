package no.difi.meldingsutveksling.receipt.strategy

import no.altinn.schemas.services.serviceengine.correspondence._2014._10.StatusV2
import no.altinn.schemas.services.serviceentity._2014._10.CorrespondenceStatusTypeV2
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.ServiceIdentifier.DPV
import no.difi.meldingsutveksling.api.ConversationService
import no.difi.meldingsutveksling.api.NextMoveQueue
import no.difi.meldingsutveksling.api.StatusStrategy
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringType.OK
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory
import no.difi.meldingsutveksling.receipt.ReceiptStatus.*
import no.difi.meldingsutveksling.sbd.SBDFactory
import no.difi.meldingsutveksling.status.Conversation
import no.difi.meldingsutveksling.status.ConversationMarker.markerFrom
import no.difi.meldingsutveksling.status.MessageStatus
import no.difi.meldingsutveksling.status.MessageStatusFactory
import no.difi.meldingsutveksling.util.logger
import org.springframework.stereotype.Component

@Component
class DpvStatusStrategy(private val correspondencyAgencyMessageFactory: CorrespondenceAgencyMessageFactory,
                        private val correspondenceAgencyClient: CorrespondenceAgencyClient,
                        private val conversationService: ConversationService,
                        private val messageStatusFactory: MessageStatusFactory,
                        private val sbdFactory: SBDFactory,
                        private val properties: IntegrasjonspunktProperties,
                        private val nextMoveQueue: NextMoveQueue) : StatusStrategy {

    val log = logger()

    override fun checkStatus(conversations: MutableSet<Conversation>) {
        log.debug("Checking status for ${conversations.size} DPV messages..")
        val request = correspondencyAgencyMessageFactory.createReceiptRequest(conversations)
        val result = correspondenceAgencyClient.sendStatusHistoryRequest(request)
        result?.correspondenceStatusInformation?.value?.correspondenceStatusDetailsList?.value?.statusV2?.forEach { s ->
            conversations.find { it.messageId == s.sendersReference.value }?.let {
                updateStatus(it, s)
            }
        }
    }

    private fun updateStatus(c: Conversation, status: StatusV2) {
        log.debug(markerFrom(c), "Checking status for message [id=${c.messageId}, conversationId=${c.conversationId}]")
        status.statusChanges.value.statusChangeV2.forEach {
            val mappedStatus = when (it.statusType.value()) {
                CorrespondenceStatusTypeV2.CREATED.value() -> LEVERT
                CorrespondenceStatusTypeV2.READ.value() -> LEST
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