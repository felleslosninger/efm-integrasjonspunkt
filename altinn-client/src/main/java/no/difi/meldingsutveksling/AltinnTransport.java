package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.slf4j.Marker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Transport implementation for Altinn message service.
 */
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class AltinnTransport {

    private final UUIDGenerator uuidGenerator;
    private final AltinnWsClient client;
    private final IntegrasjonspunktProperties props;
    private final SBDService sbdService;

    /**
     * @param sbd An SBD with a payload consisting of an CMS encrypted ASIC package
     */
    public void send(final StandardBusinessDocument sbd) {
        UploadRequest request = new AltinnWsRequest(getSendersReference(sbd), sbd);
        client.send(request);
    }

    /**
     * @param sbd             An SBD with a payload consisting of metadata only
     * @param asicInputStream InputStream pointing to the encrypted ASiC package
     */
    public void send(StandardBusinessDocument sbd, InputStream asicInputStream) {
        UploadRequest request = new AltinnWsRequest(getSendersReference(sbd), sbd, asicInputStream);
        client.send(request);
    }

    private String getSendersReference(StandardBusinessDocument sbd) {
        Optional<Scope> mcScope = SBDUtil.getOptionalMessageChannel(sbd);
        if (mcScope.isPresent() &&
                (SBDUtil.isStatus(sbd) || SBDUtil.isReceipt(sbd)) &&
                (isNullOrEmpty(props.getDpo().getMessageChannel()) ||
                        !mcScope.get().getIdentifier().equals(props.getDpo().getMessageChannel()))) {
            return mcScope.get().getIdentifier();
        }
        return uuidGenerator.generate();
    }

    private class AltinnWsRequest implements UploadRequest {

        private final String senderReference;
        private final StandardBusinessDocument sbd;
        private InputStream asicInputStream;

        public AltinnWsRequest(String senderReference, StandardBusinessDocument sbd) {
            this.senderReference = senderReference;
            this.sbd = sbd;
        }

        public AltinnWsRequest(String senderReference, StandardBusinessDocument sbd, InputStream is) {
            this.senderReference = senderReference;
            this.sbd = sbd;
            this.asicInputStream = is;
        }

        @Override
        public String getSender() {
            return sbdService.getSenderIdentifier(sbd);
        }

        @Override
        public String getReceiver() {
            return sbdService.getReceiverIdentifier(sbd);
        }

        @Override
        public String getSenderReference() {
            return senderReference;
        }

        @Override
        public StandardBusinessDocument getPayload() {
            return sbd;
        }

        @Override
        public InputStream getAsicInputStream() {
            return this.asicInputStream;
        }

        /**
         * Delegates creation of logstash markers to StandardBusinessDocument
         *
         * @return Logstash markers to identify a EduMessage
         */
        @Override
        public Marker getMarkers() {
            return SBDUtil.getMessageInfo(sbd).createLogstashMarkers();
        }
    }
}
