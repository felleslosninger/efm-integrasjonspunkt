package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.AltinnTransport;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.api.DpoConversationStrategy;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.dpo.MessageChannelEntry;
import no.difi.meldingsutveksling.dpo.MessageChannelRepository;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import no.difi.meldingsutveksling.sbd.ScopeFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import static no.difi.meldingsutveksling.logging.NextMoveMessageMarkers.markerFrom;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@Slf4j
@Order
public class DpoConversationStrategyImpl implements DpoConversationStrategy {

    private final AltinnTransport transport;
    private final AsicHandler asicHandler;
    private final PromiseMaker promiseMaker;
    private final SBDUtil sbdUtil;
    private final MessageChannelRepository messageChannelRepository;

    @Override
    @Transactional
    @Timed
    public void send(@NotNull NextMoveOutMessage message) {
        ifReceipt(message, mc -> message.getSbd().getScopes().add(ScopeFactory.fromIdentifier(ScopeType.MESSAGE_CHANNEL, mc.getChannel())));
        // Strip main identifier if part identifier exists
        message.getSbd().getPartIdentifier().ifPresent(o -> {
            Organisasjonsnummer org = Organisasjonsnummer.from(o);
            message.getSbd().getStandardBusinessDocumentHeader().getFirstSender().ifPresent(s -> {
                s.setIdentifier(new PartnerIdentification()
                    .setValue(org.asIso6523())
                    .setAuthority(org.authority()));
            });
        });

        if (message.getFiles() == null || message.getFiles().isEmpty()) {
            transport.send(message.getSbd());
            return;
        }

        try {
            promiseMaker.promise(reject -> {
                try (InputStream is = asicHandler.createEncryptedAsic(message, reject)) {
                    transport.send(message.getSbd(), is);
                    return null;
                } catch (IOException e) {
                    throw new NextMoveRuntimeException(String.format("Error sending message with messageId=%s to Altinn", message.getMessageId()), e);
                }
            }).await();
        } catch (Exception e) {
            Audit.error(String.format("Error sending message with messageId=%s to Altinn", message.getMessageId()), markerFrom(message), e);
            throw e;
        }

        Audit.info(String.format("Message [id=%s, serviceIdentifier=%s] sent to altinn",
                message.getMessageId(), message.getServiceIdentifier()),
                markerFrom(message));
        ifReceipt(message, mc -> messageChannelRepository.deleteByMessageId(mc.getMessageId()));
    }

    private void ifReceipt(NextMoveOutMessage message, Consumer<MessageChannelEntry> consumer) {
        if (sbdUtil.isReceipt(message.getSbd()) && message.getSbd().getBusinessMessage() instanceof ArkivmeldingKvitteringMessage) {
            ArkivmeldingKvitteringMessage receipt = (ArkivmeldingKvitteringMessage) message.getSbd().getBusinessMessage();
            messageChannelRepository.findByMessageId(receipt.getRelatedToMessageId()).ifPresent(consumer);
        }
    }

}
