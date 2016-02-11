package no.difi.meldingsutveksling.noarkexchange.receive;

import no.difi.meldingsutveksling.StandardBusinessDocumentConverter;
import no.difi.meldingsutveksling.domain.sbdh.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * The idea behind this queue is to avoid loosing messages before they are saved in Noark System.
 *
 * The way it works is that any exceptions that happens in after a message is put on the queue is re-sent to the JMS
 * listener. If the application is restarted the message is also resent.
 *
 * The JMS listener has the responsibility is to forward the message to the archive system.
 *
 */
@Component
public class InternalQueue {
    private static int attempts = 0;

    @Autowired
    JmsTemplate jmsTemplate;
    private String DESTINATION = "mailbox-destination";

    @JmsListener(destination = "mailbox-destination", containerFactory = "myJmsContainerFactory")
    public void receiveMessage(byte[] message, Session session) {
        Document document = new StandardBusinessDocumentConverter().unmarshallFrom(message);
        System.out.println("<<< Received message <" + document + ">");
    }

    public void put(String document) {
        jmsTemplate.convertAndSend(DESTINATION, document);
    }

    public void put(Document document) {
        jmsTemplate.convertAndSend(DESTINATION, new StandardBusinessDocumentConverter().marshallToBytes(document));
    }
}
