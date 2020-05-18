package no.difi.meldingsutveksling.receipt

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties
import no.difi.meldingsutveksling.util.logger
import org.apache.activemq.ActiveMQSession
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.core.MessagePostProcessor
import org.springframework.jms.support.JmsUtils
import org.springframework.stereotype.Component
import java.util.*
import javax.jms.TextMessage

@Component
open class StatusQueue(private val jmsTemplate: JmsTemplate,
                       private val props: IntegrasjonspunktProperties) {

    val log = logger()
    private val queueName: String = "STATUS"

    init {
        jmsTemplate.receiveTimeout = 500L;
        jmsTemplate.sessionAcknowledgeMode = 2
    }

    fun enqueueStatus(status: MessageStatus, conversation: Conversation) {
        if (props.feature.statusQueueIncludes.contains(conversation.serviceIdentifier)) {
            conversation.messageStatuses.first { it.status == status.status }?.id?.let {
                jmsTemplate.send(queueName) { c ->
                    val m = c.createTextMessage(it.toString())
                    m.jmsCorrelationID = it.toString()
                    m
                }
            }
        }
    }

    open fun receiveStatus(): Optional<Long> {
        return jmsTemplate.browse(queueName) { _, b ->
            b.enumeration.nextElement() as? TextMessage
        }?.let {
            log.debug("Message on status queue: $it")
            Optional.of(it.text.toLong())
        } ?: Optional.empty()
    }

    fun removeStatus(id: Long): Boolean {
        val selector = "JMSCorrelationID='${id}'"
        return jmsTemplate.browseSelected(queueName, selector) { _, b ->
            b.enumeration.nextElement() as? TextMessage
        }?.let {
            jmsTemplate.receiveSelected(queueName, selector)
            true
        } ?: false
    }

}