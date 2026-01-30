package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ResponseStatusSender {

    private final ResponseStatusSenderProxy proxy;
    private final IntegrasjonspunktProperties props;
    private final Logger log = LoggerFactory.getLogger(ResponseStatusSender.class);

    public ResponseStatusSender(ResponseStatusSenderProxy proxy, IntegrasjonspunktProperties props) {
        this.proxy = proxy;
        this.props = props;
    }

    public void queue(StandardBusinessDocument sbd, ServiceIdentifier si, ReceiptStatus status) {
        if (props.getNextmove().getStatusServices().contains(si)) {
            Thread t = new Thread(() -> {
                try {
                    proxy.queue(sbd, si, status);
                } catch (Throwable t1) {
                    log.error("Error sending status message", t1);
                }
            }, "response-status-sender");
            t.setDaemon(true);
            t.start();
        }
    }

}
