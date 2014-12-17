package no.difi.meldingsutveksling.adresseregister;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextStartedEvent;

import java.util.logging.Logger;

/**
 * This listener triggers on the application start event and makes sure that initial data is inserted into the
 * database.
 *
 * @author Glenn Bech
 */
public class CustomApplicationListener implements org.springframework.context.ApplicationListener {

    private final Logger log = Logger.getLogger(CustomApplicationListener.class.getName());

    @Override
    public void onApplicationEvent(final ApplicationEvent event) {
        if (event instanceof ContextStartedEvent) {
            // generate data (disabled for now)
            log.info("Context started.");
        }
    }
}


