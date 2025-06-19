package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.altinnv3.DPO.AltinnBroker;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.api.DpoConversationStrategy;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.altinnv3.DPO.MessageChannelEntry;
import no.difi.meldingsutveksling.altinnv3.DPO.MessageChannelRepository;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.sbd.ScopeFactory;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@Slf4j
@Order
public class DpoConversationStrategyImpl implements DpoConversationStrategy {

    private final AltinnBroker altinnBroker;
    private final AsicHandler asicHandler;
    private final PromiseMaker promiseMaker;
    private final MessageChannelRepository messageChannelRepository;

    @Override
    @Transactional
    @Timed
    public void send(@NotNull NextMoveOutMessage message) {
        ifReceipt(message, mc -> message.getSbd().addScope(ScopeFactory.fromIdentifier(ScopeType.MESSAGE_CHANNEL, mc.getChannel())));
        // Strip main identifier if part identifier exists
        SBDUtil.getPartIdentifier(message.getSbd()).ifPresent(o -> message.getSbd().setSenderIdentifier(o));

        if (message.getFiles() == null || message.getFiles().isEmpty()) {
            altinnBroker.send(message.getSbd());
            return;
        }

        try {
            promiseMaker.promise(reject -> {
                Resource encryptedAsic = asicHandler.createCmsEncryptedAsice(message, reject);
                altinnBroker.send(message.getSbd(), encryptedAsic);
                return null;
            }).await();
        } catch (Exception e) {
            Audit.error("Error sending message with messageId=%s to Altinn".formatted(message.getMessageId()), markerFrom(message), e);
            throw e;
        }

        Audit.info("Message [id=%s, serviceIdentifier=%s] sent to altinn".formatted(
                message.getMessageId(), message.getServiceIdentifier()),
                markerFrom(message));
        ifReceipt(message, mc -> messageChannelRepository.deleteByMessageId(mc.getMessageId()));
    }

    private void ifReceipt(NextMoveOutMessage message, Consumer<MessageChannelEntry> consumer) {
        if (SBDUtil.isReceipt(message.getSbd())) {
            message.getSbd().getBusinessMessage(ArkivmeldingKvitteringMessage.class)
                    .flatMap(receipt -> messageChannelRepository.findByMessageId(receipt.getRelatedToMessageId()))
                    .ifPresent(consumer);
        }
    }
}
