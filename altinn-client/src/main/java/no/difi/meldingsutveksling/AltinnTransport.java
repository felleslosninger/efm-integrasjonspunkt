package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.shipping.UploadRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

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

    /**
     * @param sbd An SBD with a payload consisting of an CMS encrypted ASIC package
     */
    public void send(final StandardBusinessDocument sbd) {
        UploadRequest request = new AltinnWsRequest(getSendersReference(sbd), sbd);
        client.send(request);
    }

    /**
     * @param sbd  An SBD with a payload consisting of metadata only
     * @param asic Resource pointing to the encrypted ASiC package
     */
    public void send(StandardBusinessDocument sbd, Resource asic) {
        UploadRequest request = new AltinnWsRequest(getSendersReference(sbd), sbd, asic);
        try {
            client.send(request);
        } catch (Exception e) {
            throw new NextMoveRuntimeException(String.format("Error sending message with messageId=%s to Altinn", sbd.getMessageId()), e);
        }
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
}
