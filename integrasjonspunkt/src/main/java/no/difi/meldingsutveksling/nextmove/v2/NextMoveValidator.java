package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.*;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.message.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.validation.Asserter;
import no.difi.meldingsutveksling.validation.group.ValidationGroupFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.DocumentType.ARKIVMELDING;
import static no.difi.meldingsutveksling.DocumentType.DIGITAL;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;


@Component
@Slf4j
@RequiredArgsConstructor
public class NextMoveValidator {

    private final NextMoveServiceRecordProvider serviceRecordProvider;
    private final NextMoveMessageOutRepository messageRepo;
    private final ServiceIdentifierService serviceIdentifierService;
    private final Asserter asserter;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final TimeToLiveHelper timeToLiveHelper;
    private final SBDUtil sbdUtil;
    private final ConversationService conversationService;
    private final ArkivmeldingUtil arkivmeldingUtil;

    void validate(StandardBusinessDocument sbd) {
        sbd.getOptionalMessageId().ifPresent(messageId -> {
                    messageRepo.findByMessageId(messageId)
                            .map(p -> {
                                throw new MessageAlreadyExistsException(messageId);
                            });
                    if (!sbdUtil.isStatus(sbd)) {
                        conversationService.findConversation(messageId)
                                .map(c -> {
                                    throw new MessageAlreadyExistsException(messageId);
                                });
                    }
                }
        );

        ServiceRecord serviceRecord = serviceRecordProvider.getServiceRecord(sbd);
        ServiceIdentifier serviceIdentifier = serviceRecord.getServiceIdentifier();

        if (!serviceIdentifierService.isEnabled(serviceIdentifier)) {
            throw new ServiceNotEnabledException(serviceIdentifier);
        }

        DocumentType documentType = DocumentType.valueOf(sbd.getMessageType(), ApiType.NEXTMOVE)
                .orElseThrow(() -> new UnknownNextMoveDocumentTypeException(sbd.getMessageType()));

        String standard = sbd.getStandard();

        if (!documentType.fitsDocumentIdentifier(standard)) {
            throw new DocumentTypeDoNotFitDocumentStandardException(documentType, standard);
        }

        if (!serviceRecord.hasStandard(standard)) {
            throw new ReceiverDoNotAcceptDocumentStandard(standard, sbd.getProcess());
        }

        Class<?> group = ValidationGroupFactory.toServiceIdentifier(serviceIdentifier);
        asserter.isValid(sbd.getAny(), group != null ? new Class<?>[]

                {
                        group
                } : new Class<?>[0]);
    }

    @Transactional(noRollbackFor = TimeToLiveException.class)
    public void validate(NextMoveOutMessage message) {
        // Must always be at least one attachment
        StandardBusinessDocument sbd = message.getSbd();
        if (!sbdUtil.isReceipt(sbd) && (message.getFiles() == null || message.getFiles().isEmpty())) {
            throw new MissingFileException();
        }

        sbd.getExpectedResponseDateTime().ifPresent(expectedResponseDateTime -> {
            if (sbdUtil.isExpired(sbd)) {
                timeToLiveHelper.registerErrorStatusAndMessage(sbd, message.getServiceIdentifier(), message.getDirection());
                throw new TimeToLiveException(expectedResponseDateTime);
            }
        });

        if (sbdUtil.isType(message.getSbd(), ARKIVMELDING)) {
            Set<String> messageFilenames = message.getFiles().stream()
                    .map(BusinessMessageFile::getFilename)
                    .collect(Collectors.toSet());
            // Verify each file referenced in arkivmelding is uploaded
            List<String> arkivmeldingFiles = arkivmeldingUtil.getFilenames(getArkivmelding(message));

            List<String> missingFiles = arkivmeldingFiles.stream()
                    .filter(p -> !messageFilenames.contains(p))
                    .collect(Collectors.toList());

            if (!missingFiles.isEmpty()) {
                throw new MissingArkivmeldingFileException(String.join(",", missingFiles));
            }
        }

        // Validate that files given in metadata mapping exist
        if (sbdUtil.isType(message.getSbd(), DIGITAL)) {
            Set<String> messageFilenames = message.getFiles().stream()
                    .map(BusinessMessageFile::getFilename)
                    .collect(Collectors.toSet());
            DpiDigitalMessage bmsg = (DpiDigitalMessage) message.getBusinessMessage();
            Set<String> filerefs = Stream.of(bmsg.getMetadataFiler().keySet(), bmsg.getMetadataFiler().values())
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            if (!messageFilenames.containsAll(filerefs)) {
                String missing = filerefs.stream()
                        .filter(f -> !messageFilenames.contains(f))
                        .collect(Collectors.joining(", "));
                log.error("The following files were defined in metadata, but are missing as attachments: {}", missing);
                throw new FileNotFoundException(missing);
            }
        }

        if (message.getServiceIdentifier() == DPI && message.getFiles().stream()
                .noneMatch(BusinessMessageFile::getPrimaryDocument)) {
            throw new MissingPrimaryDocumentException();
        }
    }

    private Arkivmelding getArkivmelding(NextMoveOutMessage message) {
        // Arkivmelding must exist for DPO
        BusinessMessageFile arkivmeldingFile = message.getFiles().stream()
                .filter(f -> NextMoveConsts.ARKIVMELDING_FILE.equals(f.getFilename()))
                .findAny()
                .orElseThrow(MissingArkivmeldingException::new);


        try (InputStream is = new ByteArrayInputStream(optionalCryptoMessagePersister.read(message.getMessageId(), arkivmeldingFile.getIdentifier()))) {
            return arkivmeldingUtil.unmarshalArkivmelding(is);
        } catch (JAXBException | IOException e) {
            throw new NextMoveRuntimeException("Failed to get Arkivmelding", e);
        }
    }

    void validateFile(NextMoveOutMessage message, MultipartFile file) {
        Set<BusinessMessageFile> files = message.getOrCreateFiles();
        files.stream()
                .map(BusinessMessageFile::getFilename)
                .filter(fn -> fn.equals(file.getOriginalFilename()))
                .findAny()
                .ifPresent(fn -> {
                    throw new DuplicateFilenameException(file.getOriginalFilename());
                });

        if (message.isPrimaryDocument(file.getOriginalFilename()) && files.stream().anyMatch(BusinessMessageFile::getPrimaryDocument)) {
            throw new MultiplePrimaryDocumentsNotAllowedException();
        }

        List<ServiceIdentifier> requiredTitleCapabilities = asList(DPV, DPI);
        if (requiredTitleCapabilities.contains(message.getServiceIdentifier())
                && !StringUtils.hasText(file.getName())) {
            throw new MissingFileTitleException(requiredTitleCapabilities.stream()
                    .map(ServiceIdentifier::toString)
                    .collect(Collectors.joining(",")));
        }
    }
}
