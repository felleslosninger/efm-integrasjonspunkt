package no.difi.meldingsutveksling.nextmove

import no.difi.meldingsutveksling.NextMoveConsts
import no.difi.meldingsutveksling.ServiceIdentifier
import no.difi.meldingsutveksling.api.ConversationService
import no.difi.meldingsutveksling.api.MessagePersister
import no.difi.meldingsutveksling.api.NextMoveQueue
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException
import no.difi.meldingsutveksling.domain.sbdh.SBDService
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument
import no.difi.meldingsutveksling.dpo.MessageChannelEntry
import no.difi.meldingsutveksling.dpo.MessageChannelRepository
import no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageInRepository
import no.difi.meldingsutveksling.receipt.ReceiptStatus
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Component
open class NextMoveQueueImpl(
    private val messageRepo: NextMoveMessageInRepository,
    private val conversationService: ConversationService,
    private val sbdService: SBDService,
    private val messagePersister: MessagePersister,
    private val timeToLiveHelper: TimeToLiveHelper,
    private val statusSender: ResponseStatusSender,
    private val messageChannelRepository: MessageChannelRepository
) : NextMoveQueue {

    val log = LoggerFactory.getLogger(NextMoveQueueImpl::class.java)

    override fun enqueueIncomingMessage(sbd: StandardBusinessDocument, serviceIdentifier: ServiceIdentifier) {
        enqueueIncomingMessage(sbd, serviceIdentifier, null)
    }

    @Transactional
    override fun enqueueIncomingMessage(
        sbd: StandardBusinessDocument,
        serviceIdentifier: ServiceIdentifier,
        asic: Resource?
    ) {
        MDC.put(NextMoveConsts.CORRELATION_ID, sbd.messageId)
        when {
            sbd.any !is BusinessMessageAsAttachment<*> -> throw MeldingsUtvekslingRuntimeException("SBD payload not of a known type")
            sbdService.isExpired(sbd) -> {
                timeToLiveHelper.registerErrorStatusAndMessage(sbd, serviceIdentifier, ConversationDirection.INCOMING)
                return
            }
            SBDUtil.isStatus(sbd) -> {
                log.debug("Message with id=${sbd.messageId} is a receipt")
                conversationService.registerStatus(sbd.messageId, (sbd.any as StatusMessage).status)
                return
            }
        }

        if(asic != null) {
            messagePersister.write(sbd.messageId, NextMoveConsts.ASIC_FILE, asic)
        }

        if (!messageRepo.findByMessageId(sbd.messageId).isPresent) {
            val message = messageRepo.save(NextMoveInMessage.of(sbd, serviceIdentifier))

        SBDUtil.getOptionalMessageChannel(sbd).ifPresent {
            messageChannelRepository.save(MessageChannelEntry(sbd.messageId, it.identifier))
        }
        conversationService.registerConversation(
            sbd,
            serviceIdentifier,
            ConversationDirection.INCOMING,
            ReceiptStatus.INNKOMMENDE_MOTTATT
        )
        statusSender.queue(message.sbd, serviceIdentifier, ReceiptStatus.MOTTATT)
            log.info(markerFrom(message), "Message [id=${message.messageId}, serviceIdentifier=$serviceIdentifier] put on local queue",)
        } else {
            log.warn("Received duplicate message with id=${sbd.messageId}, message discarded.")
        }
    }
}
