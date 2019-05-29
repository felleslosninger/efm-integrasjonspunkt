package no.difi.meldingsutveksling.noarkexchange;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.Decryptor;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.domain.sbdh.CorrelationInformation;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.mail.MailClient;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageService;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.services.Adresseregister;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.logging.MessageMarkerFactory.markerFrom;
import static no.difi.meldingsutveksling.noarkexchange.logging.PutMessageResponseMarkers.markerFrom;

/**
 *
 */
@Component("recieveService")
@Slf4j
public class IntegrajonspunktReceiveImpl {

    private static final String OKEY_TYPE = "OK";
    private static final String OK_TYPE = OKEY_TYPE;

    private final NoarkClient localNoark;
    private final Adresseregister adresseregisterService;
    private final IntegrasjonspunktProperties properties;
    private final IntegrasjonspunktNokkel keyInfo;
    private final ConversationService conversationService;
    private final MessagePersister messagePersister;
    private final SBDReceiptFactory sbdReceiptFactory;
    private final MessageStatusFactory messageStatusFactory;
    private final SBDUtil sbdUtil;
    private final PutMessageRequestFactory putMessageRequestFactory;
    private final NextMoveMessageService nextMoveMessageService;
    private final InternalQueue internalQueue;

    public IntegrajonspunktReceiveImpl(@Qualifier("localNoark") ObjectProvider<NoarkClient> localNoark,
                                       Adresseregister adresseregisterService,
                                       IntegrasjonspunktProperties properties,
                                       IntegrasjonspunktNokkel keyInfo,
                                       ConversationService conversationService,
                                       ObjectProvider<MessagePersister> messagePersister,
                                       SBDReceiptFactory sbdReceiptFactory,
                                       MessageStatusFactory messageStatusFactory,
                                       SBDUtil sbdUtil,
                                       PutMessageRequestFactory putMessageRequestFactory,
                                       @Lazy NextMoveMessageService nextMoveMessageService,
                                       @Lazy InternalQueue internalQueue) {
        this.localNoark = localNoark.getIfAvailable();
        this.adresseregisterService = adresseregisterService;
        this.properties = properties;
        this.keyInfo = keyInfo;
        this.conversationService = conversationService;
        this.messagePersister = messagePersister.getIfUnique();
        this.sbdReceiptFactory = sbdReceiptFactory;
        this.messageStatusFactory = messageStatusFactory;
        this.sbdUtil = sbdUtil;
        this.putMessageRequestFactory = putMessageRequestFactory;
        this.nextMoveMessageService = nextMoveMessageService;
        this.internalQueue = internalQueue;
    }

    public CorrelationInformation forwardToNoarkSystem(StandardBusinessDocument sbd) throws MessageException {
        try {
            adresseregisterService.validateCertificates(sbd);
            Audit.info("Certificates validated", markerFrom(sbd));
        } catch (MessageException e) {
            Audit.error(e.getMessage(), markerFrom(sbd), e);
            throw e;
        }

        if (sbdUtil.isReceipt(sbd)) {
            Optional<Conversation> c = conversationService.registerStatus(sbd.getConversationId(), messageStatusFactory.getMessageStatus(ReceiptStatus.LEST));
            c.ifPresent(conversationService::markFinished);
            if (!properties.getFeature().isForwardReceivedAppReceipts()) {
                Audit.info("AppReceipt forwarding disabled - will not deliver to archive");
                return new CorrelationInformation();
            }
        }
        PutMessageRequestType putMessage = convertSbdToPutMessageRequest(sbd);

        forwardToNoarkSystemAndSendReceipts(sbd, putMessage);
        return new CorrelationInformation();
    }

    private PutMessageRequestType convertSbdToPutMessageRequest(StandardBusinessDocument sbd) {
        if (sbdUtil.isReceipt(sbd)) {
            ArkivmeldingKvitteringMessage message = (ArkivmeldingKvitteringMessage) sbd.getAny();
            AppReceiptType appReceiptType = AppReceiptFactory.from(message);
            PutMessageRequestType putMessage = putMessageRequestFactory.create(sbd, BestEduConverter.appReceiptAsString(appReceiptType));

            Optional<Conversation> c = conversationService.registerStatus(sbd.getConversationId(), messageStatusFactory.getMessageStatus(ReceiptStatus.LEST));
            c.ifPresent(conversationService::markFinished);

            return putMessage;
        } else {
            byte[] asicBytes;
            try {
                asicBytes = messagePersister.read(sbd.getConversationId(), NextMoveConsts.ASIC_FILE);
            } catch (IOException e) {
                throw new NextMoveRuntimeException("Unable to read persisted ASiC", e);
            }
            byte[] asic = new Decryptor(keyInfo).decrypt(asicBytes);
            Arkivmelding arkivmelding = convertAsicEntryToArkivmelding(asic);
            MeldingType meldingType = MeldingFactory.create(arkivmelding, asic);
            return putMessageRequestFactory.create(sbd, BestEduConverter.meldingTypeAsString(meldingType));
        }
    }

    private void forwardToNoarkSystemAndSendReceipts(StandardBusinessDocument sbd, PutMessageRequestType putMessage) {
        PutMessageResponseType response = localNoark.sendEduMelding(putMessage);
        if (response == null || response.getResult() == null) {
            Audit.info("Empty response from archive", markerFrom(sbd));
        } else {
            AppReceiptType result = response.getResult();
            if (result.getType().equals(OK_TYPE)) {
                Audit.info("Delivered archive", markerFrom(response));
                Optional<Conversation> c = conversationService.registerStatus(sbd.getConversationId(),
                        messageStatusFactory.getMessageStatus(ReceiptStatus.INNKOMMENDE_LEVERT));
                c.ifPresent(conversationService::markFinished);
                sendLevertStatus(sbd);
                if (localNoark instanceof MailClient && !sbdUtil.isReceipt(sbd)) {
                    // Need to send AppReceipt manually in case receiver is mail
                    putMessage.setPayload(BestEduConverter.appReceiptAsString(result));
                    PutMessageRequestWrapper putMessageWrapper = new PutMessageRequestWrapper(putMessage);
                    putMessageWrapper.swapSenderAndReceiver();
                    nextMoveMessageService.convertAndSend(putMessageWrapper);
                }
                try {
                    messagePersister.delete(sbd.getConversationId());
                } catch (IOException e) {
                    log.error(String.format("Unable to delete files for conversation with id=%s", sbd.getConversationId()), e);
                }
            } else {
                Audit.error("Unexpected response from archive", markerFrom(response));
                log.error(">>> archivesystem: " + response.getResult().getMessage().get(0).getText());
            }
        }
    }

    public void sendLevertStatus(StandardBusinessDocument sbd) {
        StandardBusinessDocument statusSbd = sbdReceiptFactory.createArkivmeldingStatusFrom(sbd, DocumentType.STATUS, ReceiptStatus.LEVERT);
        NextMoveOutMessage msg = NextMoveOutMessage.of(statusSbd, DPO);
        internalQueue.enqueueNextMove(msg);
    }

    private Arkivmelding convertAsicEntryToArkivmelding(byte[] bytes) {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (NextMoveConsts.ARKIVMELDING_FILE.equals(entry.getName())) {
                    JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{Arkivmelding.class}, null);
                    Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
                    return unMarshaller.unmarshal(new StreamSource(zipInputStream), Arkivmelding.class).getValue();
                }
            }
            throw new NextMoveRuntimeException(String.format("%s not found in ASiC", NextMoveConsts.ARKIVMELDING_FILE));
        } catch (IOException | JAXBException e) {
            throw new NextMoveRuntimeException("Unable to read arkivmelding.xml in ASiC", e);
        }
    }
}
