package no.difi.meldingsutveksling.adresseregister;

import org.springframework.context.ApplicationEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: glennbech
 * Date: 09.12.14
 * Time: 16:40
 * To change this template use File | Settings | File Templates.
 */
public class CustomApplicationListener implements org.springframework.context.ApplicationListener {

    private final Logger log = Logger.getLogger(CustomApplicationListener.class.getName());

    public void onApplicationEvent(final ApplicationEvent event) {
        log.log(Level.INFO, "Event = " + event);
    }
}


