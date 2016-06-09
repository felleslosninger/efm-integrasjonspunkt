package no.difi.meldingsutveksling.transport;


import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;


public class PostVirksomhetTransport implements Transport {


    private Logger log = LoggerFactory.getLogger(PostVirksomhetTransport.class);

    @Override
    public void send(Environment environment, EduDocument document) {
        log.debug("Post til virksomhet. Send " + document);
    }
}
