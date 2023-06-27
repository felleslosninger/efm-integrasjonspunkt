package no.difi.meldingsutveksling.noarkexchange;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.bestedu.PutMessageRequestFactory;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.dokumentpakking.service.DecryptCMSDocument;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.mail.MailClient;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringMessage;
import no.difi.meldingsutveksling.nextmove.InternalQueue;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.move.common.cert.KeystoreHelper;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
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

    private final JAXBContext jaxbContext;

    private final NoarkClient localNoark;
    private final KeystoreHelper keystoreHelper;
    private final ConversationService conversationService;
    private final MessagePersister messagePersister;
    private final SBDFactory sbdFactory;
    private final SBDService sbdService;
    private final PutMessageRequestFactory putMessageRequestFactory;
    private final NextMoveAdapter nextMoveAdapter;
    private final InternalQueue internalQueue;
    private final ConversationIdEntityRepo conversationIdEntityRepo;
    private final MeldingFactory meldingFactory;
    private final DecryptCMSDocument decryptCMSDocument;

    public IntegrajonspunktReceiveImpl(@Qualifier("localNoark") ObjectProvider<NoarkClient> localNoark,
                                       KeystoreHelper keystoreHelper,
                                       ConversationService conversationService,
                                       ObjectProvider<MessagePersister> messagePersister,
                                       SBDFactory sbdFactory,
                                       SBDService sbdService,
                                       PutMessageRequestFactory putMessageRequestFactory,
                                       @Lazy NextMoveAdapter nextMoveAdapter,
                                       @Lazy InternalQueue internalQueue,
                                       ConversationIdEntityRepo conversationIdEntityRepo,
                                       MeldingFactory meldingFactory, DecryptCMSDocument decryptCMSDocument) throws JAXBException {
        this.localNoark = localNoark.getIfAvailable();
        this.keystoreHelper = keystoreHelper;
        this.conversationService = conversationService;
        this.messagePersister = messagePersister.getIfUnique();
        this.sbdFactory = sbdFactory;
        this.sbdService = sbdService;
        this.putMessageRequestFactory = putMessageRequestFactory;
        this.nextMoveAdapter = nextMoveAdapter;
        this.internalQueue = internalQueue;
        this.conversationIdEntityRepo = conversationIdEntityRepo;
        this.meldingFactory = meldingFactory;
        this.decryptCMSDocument = decryptCMSDocument;

        this.jaxbContext = JAXBContextFactory.createContext(new Class[]{Arkivmelding.class}, null);
    }

    public void forwardToNoarkSystem(StandardBusinessDocument sbd) {
        if (sbdService.isExpired(sbd)) {
            conversationService.registerStatus(sbd.getMessageId(), ReceiptStatus.LEVETID_UTLOPT);
            try {
                messagePersister.delete(sbd.getMessageId());
            } catch (IOException e) {
                log.warn(markerFrom(sbd), "Error deleting files in IntegrajonspunktReceiveImpl.forwardToNoarkSystem() for expired message {}", sbd.getMessageId(), e);
            }
            return;
        }
        PutMessageRequestType putMessage = convertSbdToPutMessageRequest(sbd);
        forwardToNoarkSystemAndSendReceipts(sbd, putMessage);
    }

    private PutMessageRequestType convertSbdToPutMessageRequest(StandardBusinessDocument sbd) {
        if (SBDUtil.isReceipt(sbd)) {
            ArkivmeldingKvitteringMessage message = (ArkivmeldingKvitteringMessage) sbd.getAny();
            AppReceiptType appReceiptType = AppReceiptFactory.from(message);
            // ConversationId may be be overriden due to invalid UUID in corresponding outgoing message
            ConversationIdEntity convId = conversationIdEntityRepo.findByNewConversationId(sbd.getConversationId());
            if (convId != null) {
                log.warn("Found {} which maps to conversation {} with invalid UUID - overriding in AppReceipt.", sbd.getConversationId(), convId.getOldConversationId());
                PutMessageRequestType putMessage = putMessageRequestFactory.create(sbd, BestEduConverter.appReceiptAsString(appReceiptType), convId.getOldConversationId());
                conversationIdEntityRepo.delete(convId);
                return putMessage;
            }
            return putMessageRequestFactory.create(sbd, BestEduConverter.appReceiptAsString(appReceiptType));
        } else {


            Resource encryptedAsic = getEncryptedAsic(sbd);
            Resource asic = decryptCMSDocument.decrypt(DecryptCMSDocument.Input.builder()
                    .resource(encryptedAsic)
                    .keystoreHelper(keystoreHelper)
                    .build());
            try {
                Arkivmelding arkivmelding = convertAsicEntryToArkivmelding(asic);
                MeldingType meldingType = meldingFactory.create(arkivmelding, asic);
                return putMessageRequestFactory.create(sbd, BestEduConverter.meldingTypeAsString(meldingType));
            } catch (Exception e) {
                throw new NextMoveRuntimeException("Closing ASiC failed!", e);
            }
        }
    }

    private Resource getEncryptedAsic(StandardBusinessDocument sbd) {
        try {
            return messagePersister.read(sbd.getMessageId(), NextMoveConsts.ASIC_FILE);
        } catch (IOException e) {
            throw new NextMoveRuntimeException("Could not read ASiC!", e);
        }
    }

    private void forwardToNoarkSystemAndSendReceipts(StandardBusinessDocument sbd, PutMessageRequestType putMessage) {
        PutMessageResponseType response = localNoark.sendEduMelding(putMessage);
        if (response == null || response.getResult() == null) {
            Audit.info(String.format("Empty response from archive for message [id=%s]", sbd.getMessageId()), markerFrom(sbd));
        } else {
            AppReceiptType result = response.getResult();
            if (result.getType().equals(OK_TYPE)) {
                Audit.info(String.format("Message [id=%s] delivered archive", sbd.getMessageId()), markerFrom(response));
                conversationService.registerStatus(sbd.getMessageId(), ReceiptStatus.INNKOMMENDE_LEVERT);
                sendLevertStatus(sbd);
                if (localNoark instanceof MailClient && !SBDUtil.isReceipt(sbd)) {
                    // Need to send AppReceipt manually in case receiver is mail
                    putMessage.setPayload(BestEduConverter.appReceiptAsString(result));
                    PutMessageRequestWrapper putMessageWrapper = new PutMessageRequestWrapper(putMessage);
                    putMessageWrapper.swapSenderAndReceiver();
                    nextMoveAdapter.convertAndSend(putMessageWrapper);
                }
                try {
                    messagePersister.delete(sbd.getMessageId());
                } catch (IOException e) {
                    log.warn(String.format("Error deleting files in IntegrajonspunktReceiveImpl.forwardToNoarkSystemAndSendReceipts() for message with id=%s", sbd.getMessageId()), e);
                }
            } else {
                Audit.error(String.format("Unexpected response from archive for message [id=%s]", sbd.getMessageId()), markerFrom(response));
                if (!response.getResult().getMessage().isEmpty()) {
                    log.error(">>> archivesystem: " + response.getResult().getMessage().get(0).getText());
                }
            }
        }
    }

    private void sendLevertStatus(StandardBusinessDocument sbd) {
        StandardBusinessDocument statusSbd = sbdFactory.createStatusFrom(sbd, ReceiptStatus.LEVERT);
        if (statusSbd != null) {
            NextMoveOutMessage msg = NextMoveOutMessage.of(statusSbd, DPO);
            internalQueue.enqueueNextMove(msg);
        }
    }

    private Arkivmelding convertAsicEntryToArkivmelding(Resource resource) {
        try (ZipInputStream zipInputStream = new ZipInputStream(resource.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (NextMoveConsts.ARKIVMELDING_FILE.equals(entry.getName())) {
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
