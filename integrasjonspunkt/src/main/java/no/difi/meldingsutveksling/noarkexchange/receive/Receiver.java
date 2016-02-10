package no.difi.meldingsutveksling.noarkexchange.receive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.Session;

@Component
public class Receiver {
    @Autowired
    ConfigurableApplicationContext context;

    private static int attempts = 0;

    @JmsListener(destination = "mailbox-destination", containerFactory = "myJmsContainerFactory")
    public void receiveMessage(String message, Session session) {
//        if(attempts < 5) {
//            attempts++;
//            throw new RuntimeException("trying out transactions in JMS");
//        }
        System.out.println("<<< Received message <" + message + ">");
  //      context.close();
        
    }
}
