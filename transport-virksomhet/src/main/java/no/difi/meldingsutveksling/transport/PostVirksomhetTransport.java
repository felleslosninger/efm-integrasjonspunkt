package no.difi.meldingsutveksling.transport;


import no.difi.meldingsutveksling.domain.sbdh.EduDocument;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;


public class PostVirksomhetTransport implements Transport {


    private final String endpoint;
    private Logger log = LoggerFactory.getLogger(PostVirksomhetTransport.class);

    public PostVirksomhetTransport(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void send(Environment environment, EduDocument document) {
        log.debug("Post til virksomhet. Send " + document);
        throw new NotImplementedException("TODO");
    }
}
