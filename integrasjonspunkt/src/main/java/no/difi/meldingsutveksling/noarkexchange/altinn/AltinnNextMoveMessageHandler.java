package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.AltinnPackage;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpo.MessageChannelEntry;
import no.difi.meldingsutveksling.dpo.MessageChannelRepository;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringMessage;
import no.difi.meldingsutveksling.nextmove.InternalQueue;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.TimeToLiveHelper;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.NextMoveConsts.ASIC_FILE;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.nextmove.ConversationDirection.INCOMING;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class AltinnNextMoveMessageHandler implements AltinnMessageHandler {

    private final IntegrasjonspunktProperties properties;
    private final InternalQueue internalQueue;
    private final ConversationService conversationService;
    private final NextMoveQueue nextMoveQueue;
    private final MessagePersister messagePersister;
    private final SBDService sbdService;
    private final TimeToLiveHelper timeToLiveHelper;
    private final MessageChannelRepository messageChannelRepository;

    @Override
    public void handleAltinnPackage(AltinnPackage altinnPackage) throws IOException {
        StandardBusinessDocument sbd = altinnPackage.getSbd();
        String messageId = SBDUtil.getMessageId(sbd);
        log.debug(String.format("NextMove message id=%s", messageId));

        if (!isNullOrEmpty(properties.getNoarkSystem().getType()) && SBDUtil.isArkivmelding(sbd) && !SBDUtil.isStatus(sbd)) {
            if (sbdService.isExpired(sbd)) {
                timeToLiveHelper.registerErrorStatusAndMessage(sbd, DPO, INCOMING);
                if (altinnPackage.getAsicInputStream() != null) {
                    altinnPackage.getAsicInputStream().close();
                    altinnPackage.getTmpFile().delete();
                }
                return;
            }
            if (altinnPackage.getAsicInputStream() != null) {
                try (InputStream asicStream = altinnPackage.getAsicInputStream()) {
                    messagePersister.writeStream(messageId, ASIC_FILE, asicStream, -1L);
                } catch (IOException e) {
                    throw new NextMoveRuntimeException("Error persisting ASiC", e);
                } finally {
                    altinnPackage.getTmpFile().delete();
                }
            }

            SBDUtil.getOptionalMessageChannel(sbd).ifPresent(s ->
                    messageChannelRepository.save(new MessageChannelEntry(messageId, s.getIdentifier())));
            conversationService.registerConversation(sbd, DPO, INCOMING);
            internalQueue.enqueueNoark(sbd);
            conversationService.registerStatus(messageId, ReceiptStatus.INNKOMMENDE_MOTTATT);
        } else {
            nextMoveQueue.enqueueIncomingMessage(sbd, DPO, altinnPackage.getAsicInputStream());
            if (altinnPackage.getTmpFile() != null) {
                altinnPackage.getTmpFile().delete();
            }
        }

        if (SBDUtil.isReceipt(sbd)) {
            sbd.getBusinessMessage(ArkivmeldingKvitteringMessage.class).ifPresent(receipt ->
                    conversationService.registerStatus(receipt.getRelatedToMessageId(), ReceiptStatus.LEST)
            );
        }
    }

}
