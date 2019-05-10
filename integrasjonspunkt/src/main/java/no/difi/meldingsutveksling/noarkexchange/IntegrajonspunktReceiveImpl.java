package no.difi.meldingsutveksling.noarkexchange;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.ApplicationContextHolder;
import no.difi.meldingsutveksling.Decryptor;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.domain.Payload;
import no.difi.meldingsutveksling.domain.sbdh.CorrelationInformation;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.kvittering.SBDReceiptFactory;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.mail.MailClient;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatusFactory;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.services.Adresseregister;
import no.difi.meldingsutveksling.transport.Transport;
import no.difi.meldingsutveksling.transport.TransportFactory;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static no.difi.meldingsutveksling.domain.sbdh.SBDUtil.isNextMove;
import static no.difi.meldingsutveksling.domain.sbdh.SBDUtil.isReceipt;
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

    private final TransportFactory transportFactory;
    private final NoarkClient localNoark;
    private final Adresseregister adresseregisterService;
    private final IntegrasjonspunktProperties properties;
    private final IntegrasjonspunktNokkel keyInfo;
    private final ConversationService conversationService;
    private final MessageSender messageSender;
    private final EDUCoreFactory eduCoreFactory;
    private final MessagePersister messagePersister;
    private final SBDReceiptFactory sbdReceiptFactory;
    private final ApplicationContextHolder applicationContextHolder;
    private final MessageStatusFactory messageStatusFactory;

    public IntegrajonspunktReceiveImpl(ApplicationContextHolder applicationContextHolder,
                                       TransportFactory transportFactory,
                                       @Qualifier("localNoark") ObjectProvider<NoarkClient> localNoark,
                                       Adresseregister adresseregisterService,
                                       IntegrasjonspunktProperties properties,
                                       IntegrasjonspunktNokkel keyInfo,
                                       ConversationService conversationService,
                                       MessageSender messageSender,
                                       EDUCoreFactory eduCoreFactory,
                                       ObjectProvider<MessagePersister> messagePersister,
                                       SBDReceiptFactory sbdReceiptFactory, MessageStatusFactory messageStatusFactory) {
        this.applicationContextHolder = applicationContextHolder;
        this.transportFactory = transportFactory;
        this.localNoark = localNoark.getIfAvailable();
        this.adresseregisterService = adresseregisterService;
        this.properties = properties;
        this.keyInfo = keyInfo;
        this.conversationService = conversationService;
        this.messageSender = messageSender;
        this.eduCoreFactory = eduCoreFactory;
        this.messagePersister = messagePersister.getIfUnique();
        this.sbdReceiptFactory = sbdReceiptFactory;
        this.messageStatusFactory = messageStatusFactory;
    }

    public CorrelationInformation forwardToNoarkSystem(StandardBusinessDocument sbd) throws MessageException {
        try {
            adresseregisterService.validateCertificates(sbd);
            Audit.info("Certificates validated", markerFrom(sbd));
        } catch (MessageException e) {
            Audit.error(e.getMessage(), markerFrom(sbd), e);
            throw e;
        }

        if (isReceipt(sbd)) {
            Audit.info("Messagetype Receipt", markerFrom(sbd));
            return new CorrelationInformation();
        }

        EDUCore eduCore;
        if (isNextMove(sbd)) {
            eduCore = convertNextMoveToEducore(sbd);
        } else {
            Payload payload = sbd.getPayload();
            byte[] decryptedAsicPackage = decrypt(payload);
            eduCore = convertAsicEntrytoEduCore(decryptedAsicPackage);
            if (PayloadUtil.isAppReceipt(eduCore.getPayload())) {
                Audit.info("AppReceipt extracted", markerFrom(sbd));
                Optional<Conversation> c = conversationService.registerStatus(eduCore.getId(), messageStatusFactory.getMessageStatus(ReceiptStatus.LEST));
                c.ifPresent(conversationService::markFinished);
                if (!properties.getFeature().isForwardReceivedAppReceipts()) {
                    Audit.info("AppReceipt forwarding disabled - will not deliver to archive");
                    return new CorrelationInformation();
                }
                // Marshall back and forth to avoid missing xml tag issues
                AppReceiptType appReceipt = EDUCoreConverter.payloadAsAppReceipt(eduCore.getPayload());
                eduCore.setPayload(EDUCoreConverter.appReceiptAsString(appReceipt));
            } else {
                Audit.info("EDU Document extracted", markerFrom(sbd));
            }
        }

        forwardToNoarkSystemAndSendReceipts(sbd, eduCore);
        return new CorrelationInformation();
    }

    private EDUCore convertNextMoveToEducore(StandardBusinessDocument sbd) {
        byte[] asicBytes;
        try {
            asicBytes = messagePersister.read(sbd.getConversationId(), NextMoveConsts.ASIC_FILE);
        } catch (IOException e) {
            throw new NextMoveRuntimeException("Unable to read persisted ASiC", e);
        }
        byte[] asic = new Decryptor(keyInfo).decrypt(asicBytes);
        Arkivmelding arkivmelding = convertAsicEntryToArkivmelding(asic);

        EDUCore eduCore = eduCoreFactory.create(sbd, arkivmelding, asic);
        Optional<Conversation> c = conversationService.registerStatus(eduCore.getId(), messageStatusFactory.getMessageStatus(ReceiptStatus.LEST));
        c.ifPresent(conversationService::markFinished);

        return eduCore;
    }

    public byte[] decrypt(Payload payload) {
        byte[] cmsEncZip = DatatypeConverter.parseBase64Binary(payload.getContent());
        return new Decryptor(keyInfo).decrypt(cmsEncZip);
    }

    public void forwardToNoarkSystemAndSendReceipts(StandardBusinessDocument sbd, EDUCore eduCore) {
        PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(eduCore);
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
                sendReceiptOpen(sbd);
                if (localNoark instanceof MailClient && eduCore.getMessageType() != EDUCore.MessageType.APPRECEIPT) {
                    // Need to send AppReceipt manually in case receiver is mail
                    eduCore.swapSenderAndReceiver();
                    eduCore.setMessageType(EDUCore.MessageType.APPRECEIPT);
                    eduCore.setPayload(result);
                    messageSender.sendMessage(eduCore);
                }
                if (isNextMove(sbd)) {
                    try {
                        messagePersister.delete(sbd.getConversationId());
                    } catch (IOException e) {
                        log.error(String.format("Unable to delete files for conversation with id=%s", sbd.getConversationId()), e);
                    }
                }
            } else {
                Audit.error("Unexpected response from archive", markerFrom(response));
                log.error(">>> archivesystem: " + response.getResult().getMessage().get(0).getText());
            }
        }

    }

    public void sendReceiptOpen(StandardBusinessDocument inputDocument) {
        StandardBusinessDocument doc = sbdReceiptFactory.createAapningskvittering(inputDocument.getMessageInfo(), keyInfo);
        sendReceipt(doc);
    }

    private void sendReceipt(StandardBusinessDocument receipt) {
        Transport t = transportFactory.createTransport(receipt);
        t.send(applicationContextHolder.getApplicationContext(), receipt);
    }

    public EDUCore convertAsicEntrytoEduCore(byte[] bytes) throws MessageException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if ("best_edu.xml".equals(entry.getName())) {
                    JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{EDUCore.class}, null);
                    Unmarshaller unMarshaller = jaxbContext.createUnmarshaller();
                    return unMarshaller.unmarshal(new StreamSource(zipInputStream), EDUCore.class).getValue();
                }
            }
        } catch (IOException | JAXBException e) {
            throw new MessageException(e, StatusMessage.UNABLE_TO_EXTRACT_BEST_EDU);
        }
        throw new MessageException(StatusMessage.UNABLE_TO_EXTRACT_BEST_EDU);
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
