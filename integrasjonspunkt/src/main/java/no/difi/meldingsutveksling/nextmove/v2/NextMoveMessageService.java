package no.difi.meldingsutveksling.nextmove.v2;

import com.google.common.collect.Sets;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateSBD;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.arkivmelding.ArkivmeldingFactory;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.ConversationNotFoundException;
import no.difi.meldingsutveksling.exceptions.MessagePersistException;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.message.CryptoMessagePersister;
import no.difi.meldingsutveksling.noarkexchange.*;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.receipt.ConversationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.google.common.base.Strings.emptyToNull;
import static no.difi.meldingsutveksling.NextMoveConsts.ARKIVMELDING_FILE;

@Component
@Slf4j
@RequiredArgsConstructor
public class NextMoveMessageService {

    private final NextMoveValidator validator;
    private final NextMoveOutMessageFactory nextMoveOutMessageFactory;
    private final CryptoMessagePersister cryptoMessagePersister;
    private final NextMoveMessageOutRepository messageRepo;
    private final InternalQueue internalQueue;
    private final ConversationService conversationService;
    private final ArkivmeldingFactory arkivmeldingFactory;
    private final CreateSBD createSBD;
    private final IntegrasjonspunktProperties properties;

    NextMoveOutMessage getMessage(String conversationId) {
        return messageRepo.findByConversationId(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    Page<NextMoveOutMessage> findMessages(Predicate predicate, Pageable pageable) {
        return messageRepo.findAll(predicate, pageable);
    }

    public NextMoveOutMessage createMessage(StandardBusinessDocument sbd) {
        validator.validate(sbd);
        NextMoveOutMessage message = nextMoveOutMessageFactory.getNextMoveOutMessage(sbd);
        messageRepo.save(message);
        conversationService.registerConversation(message);
        return message;
    }

    public void addFile(NextMoveOutMessage message, MultipartFile file) {
        validator.validateFile(message, file);

        String identifier = persistFile(message, file);

        message.getOrCreateFiles().add(new BusinessMessageFile()
                .setIdentifier(identifier)
                .setTitle(emptyToNull(file.getName()))
                .setFilename(file.getOriginalFilename())
                .setMimetype(getMimeType(file.getContentType(), file.getOriginalFilename()))
                .setPrimaryDocument(message.isPrimaryDocument(file.getOriginalFilename())));

        messageRepo.save(message);
    }

    private String persistFile(NextMoveOutMessage message, MultipartFile file) {
        String identifier = UUID.randomUUID().toString();

        try {
            cryptoMessagePersister.writeStream(message.getConversationId(), identifier, file.getInputStream(), file.getSize());
        } catch (IOException e) {
            throw new MessagePersistException(file.getOriginalFilename());
        }

        return identifier;
    }

    private String getMimeType(String contentType, String filename) {
        if (MediaType.APPLICATION_OCTET_STREAM_VALUE.equals(contentType)) {
            String ext = Stream.of(filename.split(".")).reduce((a, b) -> b).orElse("pdf");
            return MimeTypeExtensionMapper.getMimetype(ext);
        }

        return contentType;
    }

    public void sendMessage(NextMoveOutMessage message) {
        validator.validate(message);
        internalQueue.enqueueNextMove2(message);
    }

    @Transactional
    public PutMessageResponseType convertAndSend(PutMessageRequestWrapper message) {
        NextMoveOutMessage nextMoveMessage;
        if (PayloadUtil.isAppReceipt(message.getPayload())) {
            nextMoveMessage = convertAppReceipt(message);
        } else {
            try {
                nextMoveMessage = convertEduMessage(message);
            } catch (PayloadException e) {
                log.error("Error parsing payload", e);
                return PutMessageResponseFactory.createErrorResponse(e.getLocalizedMessage());
            } catch (JAXBException e) {
                log.error("Error marshalling arkivmelding converted from EDU document", e);
                return PutMessageResponseFactory.createErrorResponse("Error converting to arkivmelding");
            }
        }

        sendMessage(nextMoveMessage);

        return PutMessageResponseFactory.createOkResponse();
    }

    private NextMoveOutMessage convertAppReceipt(PutMessageRequestWrapper message) {
        AppReceiptType appReceiptType = EDUCoreConverter.payloadAsAppReceipt(message.getPayload());
        ArkivmeldingKvitteringMessage receipt = new ArkivmeldingKvitteringMessage(appReceiptType.getType(), Sets.newHashSet());
        appReceiptType.getMessage().forEach(sm -> receipt.getMessages().add(new KvitteringStatusMessage(sm.getCode(), sm.getText())));

        StandardBusinessDocument sbd = createSBD.createNextMoveSBD(Organisasjonsnummer.from(message.getSenderPartynumber()),
                Organisasjonsnummer.from(message.getReceiverPartyNumber()),
                message.getConversationId(),
                properties.getArkivmelding().getReceiptProcess(),
                DocumentType.ARKIVMELDING_KVITTERING,
                receipt);
        return createMessage(sbd);
    }

    private NextMoveOutMessage convertEduMessage(PutMessageRequestWrapper message) throws PayloadException, JAXBException {
        List<NoarkDocument> noarkDocuments = PayloadUtil.parsePayloadForDocuments(message.getPayload());

        Arkivmelding arkivmelding = arkivmeldingFactory.createArkivmeldingAndWriteFiles(message);
        byte[] arkivmeldingBytes = ArkivmeldingUtil.marshalArkivmelding(arkivmelding);

        StandardBusinessDocument sbd = createSBD.createNextMoveSBD(Organisasjonsnummer.from(message.getSenderPartynumber()),
                Organisasjonsnummer.from(message.getReceiverPartyNumber()),
                message.getConversationId(),
                properties.getArkivmelding().getDefaultProcess(),
                DocumentType.ARKIVMELDING,
                new ArkivmeldingMessage()
                        .setPrimaerDokumentNavn(ARKIVMELDING_FILE)
        );
        NextMoveOutMessage nextMoveMessage = createMessage(sbd);

        noarkDocuments.forEach(d -> {
            BasicNextMoveFile nmf = BasicNextMoveFile.of(d.getTitle(), d.getFilename(), d.getContentType(), Base64.getDecoder().decode(d.getContent()));
            addFile(nextMoveMessage, nmf);
        });

        BasicNextMoveFile arkivmeldingFile = BasicNextMoveFile.of(ARKIVMELDING_FILE,
                ARKIVMELDING_FILE, MimeTypeExtensionMapper.getMimetype("xml"), arkivmeldingBytes);
        addFile(nextMoveMessage, arkivmeldingFile);

        return nextMoveMessage;
    }
}
