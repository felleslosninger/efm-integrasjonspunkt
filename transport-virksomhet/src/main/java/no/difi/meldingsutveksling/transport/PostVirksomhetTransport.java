package no.difi.meldingsutveksling.transport;


import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.Environment;


public class PostVirksomhetTransport implements Transport {

    private static final Log log = LogFactory.getLog(PostVirksomhetTransport.class);

    @Override
    public void send(Environment environment, EduDocument document) {
        log.debug("Post til virksomhet. Send " + document);
    }
}
