package no.difi.meldingsutveksling.transport;


import no.difi.meldingsutveksling.noarkexchange.PayloadException;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyMessageFactory;
import no.difi.meldingsutveksling.ptv.CorrespondenceRequest;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;


public class PostVirksomhet {


    private final String endpoint;
    private Logger log = LoggerFactory.getLogger(PostVirksomhet.class);
    private CorrespondenceAgencyConfiguration postConfig;

    public PostVirksomhet(String endpoint) {
        this.endpoint = endpoint;
    }

    public void send(Environment environment, PutMessageRequestWrapper putMessageRequestWrapper) {
        CorrespondenceAgencyClient client = new CorrespondenceAgencyClient();
        try {
            final CorrespondenceRequest.Builder builder = new CorrespondenceRequest.Builder().withUsername(environment.getProperty("post.virksomhet.username"));
            client.send(builder.withPayload(CorrespondenceAgencyMessageFactory.create(postConfig, putMessageRequestWrapper)).build());
        } catch (PayloadException e) {
            e.printStackTrace();
        }


//        log.debug("Post til virksomhet. Send " + document);
        throw new NotImplementedException("TODO");
    }
}
