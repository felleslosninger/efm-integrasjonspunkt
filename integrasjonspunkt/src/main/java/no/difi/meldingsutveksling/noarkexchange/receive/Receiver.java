package no.difi.meldingsutveksling.noarkexchange.receive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Created by kons-mho on 05.02.2016.
 */
@Component
public class Receiver {
    @Autowired
    ConfigurableApplicationContext context;

    @JmsListener(destination = "mailbox-destination", containerFactory = "myJmsContainerFactory")
    public void receiveMessage(String message) {
        System.out.println("<<< Received message <" + message + ">");
        context.close();
        
    }
}
