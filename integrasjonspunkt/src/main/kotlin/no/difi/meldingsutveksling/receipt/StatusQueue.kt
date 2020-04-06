package no.difi.meldingsutveksling.receipt

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
open class StatusQueue(private val jmsTemplate: JmsTemplate,
                       private val props: IntegrasjonspunktProperties) {

    private val queueName: String = "STATUS"

    fun enqueueStatus(status: MessageStatus, conversation: Conversation) {
        if (props.feature.statusQueueIncludes.contains(conversation.serviceIdentifier)) {
            conversation.messageStatuses.first { it.status == status.status }?.id?.let {
                jmsTemplate.convertAndSend(queueName, it.toString())
            }
        }
    }

    open fun receiveStatus(): Optional<Long> {
        jmsTemplate.receiveTimeout = JmsTemplate.RECEIVE_TIMEOUT_NO_WAIT
        jmsTemplate.deliveryMode
        return jmsTemplate.receiveAndConvert(queueName)?.let {
            Optional.of((it as String).toLong())
        } ?: Optional.empty()
    }
}