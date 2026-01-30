package no.difi.meldingsutveksling.ks.fiksio;

import jakarta.annotation.PostConstruct;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.DpfioPolling;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.nextmove.FiksIoMessage;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.SvarSender;
import no.ks.fiks.io.client.model.MottattMelding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = {"difi.move.feature.enableDPFIO"}, havingValue = "true")
public class FiksIoSubscriber implements DpfioPolling {

    private static final Logger log = LoggerFactory.getLogger(FiksIoSubscriber.class);

    private final FiksIOKlient fiksIOKlient;
    private final SBDFactory sbdFactory;
    private final IntegrasjonspunktProperties props;
    private final NextMoveQueue nextMoveQueue;

    public FiksIoSubscriber(FiksIOKlient fiksIOKlient,
                            SBDFactory sbdFactory,
                            IntegrasjonspunktProperties props,
                            NextMoveQueue nextMoveQueue) {
        this.fiksIOKlient = fiksIOKlient;
        this.sbdFactory = sbdFactory;
        this.props = props;
        this.nextMoveQueue = nextMoveQueue;
    }

    @PostConstruct
    public void registerSubscriber() {
        if (props.getFiks().getIo().getSenderOrgnr() == null || props.getFiks().getIo().getSenderOrgnr().isBlank()) {
            throw new IllegalArgumentException("difi.move.fiks.io.sender-orgnr must not be null");
        }
        fiksIOKlient.newSubscription((mottattMelding, svarSender) -> handleMessage(mottattMelding, svarSender));
    }

    private void handleMessage(MottattMelding mottattMelding, SvarSender svarSender) {
        log.debug("FiksIO: Received message with fiksId={} protocol={}", mottattMelding.getMeldingId(), mottattMelding.getMeldingType());
        var sbd = sbdFactory.createNextMoveSBD(
            Iso6523.of(ICD.NO_ORG, props.getFiks().getIo().getSenderOrgnr()),
            Iso6523.of(ICD.NO_ORG, props.getOrg().getNumber()),
            mottattMelding.getMeldingId().toString(),
            mottattMelding.getMeldingId().toString(),
            mottattMelding.getMeldingType(),
            mottattMelding.getMeldingType(),
            MessageType.FIKSIO,
            new FiksIoMessage()
        );

        nextMoveQueue.enqueueIncomingMessage(
            sbd,
            ServiceIdentifier.DPFIO,
            new InputStreamResource(mottattMelding.getKryptertStream())
        );
        svarSender.ack();
    }

    @Override
    public void poll() {
        // noop
    }

}
