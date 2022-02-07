package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.*;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.validation.Asserter;
import no.difi.meldingsutveksling.validation.IntegrasjonspunktCertificateValidator;
import no.difi.meldingsutveksling.validation.VirksertCertificateException;
import no.difi.meldingsutveksling.validation.group.ValidationGroupFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateExpiredException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.MessageType.ARKIVMELDING;
import static no.difi.meldingsutveksling.MessageType.DIGITAL;
import static no.difi.meldingsutveksling.ServiceIdentifier.*;


@Component
@Slf4j
@RequiredArgsConstructor
public class NextMoveValidator {

    private final ServiceRecordProvider serviceRecordProvider;
    private final NextMoveMessageOutRepository messageRepo;
    private final ConversationStrategyFactory conversationStrategyFactory;
    private final Asserter asserter;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final TimeToLiveHelper timeToLiveHelper;
    private final SBDService sbdService;
    private final ConversationService conversationService;
    private final ArkivmeldingUtil arkivmeldingUtil;
    private final NextMoveFileSizeValidator fileSizeValidator;
    private final IntegrasjonspunktProperties props;
    private final ObjectProvider<IntegrasjonspunktCertificateValidator> certificateValidator;
    private final ObjectProvider<SvarUtService> svarUtService;

    void validate(StandardBusinessDocument sbd) {

        sbd.getMessageId().ifPresent(messageId -> {
                    messageRepo.findByMessageId(messageId)
                            .ifPresent(p -> {
                                throw new MessageAlreadyExistsException(messageId);
                            });
                    if (!SBDUtil.isStatus(sbd)) {
                        conversationService.findConversation(messageId)
                                .ifPresent(c -> {
                                    throw new MessageAlreadyExistsException(messageId);
                                });
                    }
                }
        );

        ServiceRecord serviceRecord = serviceRecordProvider.getServiceRecord(sbd);
        ServiceIdentifier serviceIdentifier = serviceRecord.getServiceIdentifier();

        validateCertificate(serviceIdentifier);

        if (!conversationStrategyFactory.isEnabled(serviceIdentifier)) {
            throw new ServiceNotEnabledException(serviceIdentifier);
        }

        MessageType messageType = SBDUtil.getOptionalMessageType(sbd)
                .filter(p -> p.getApi() == ApiType.NEXTMOVE)
                .orElseThrow(() -> new UnknownMessageTypeException(sbd.getType().orElse("null")));

        String documentType = SBDUtil.getDocumentType(sbd);

        if (!messageType.fitsDocumentIdentifier(documentType) && serviceRecord.getServiceIdentifier() != DPFIO) {
            throw new MessageTypeDoesNotFitDocumentTypeException(messageType, documentType);
        }

        if (serviceRecord.getServiceIdentifier() == DPO && !isNullOrEmpty(props.getDpo().getMessageChannel())) {
            Optional<Scope> mc = SBDUtil.getOptionalMessageChannel(sbd);
            if (mc.isPresent() && !mc.get().getIdentifier().equals(props.getDpo().getMessageChannel())) {
                throw new MessageChannelInvalidException(props.getDpo().getMessageChannel(), mc.get().getIdentifier());
            }
        }

        validateDpfForsendelseType(sbd, serviceRecord);

        Class<?> group = ValidationGroupFactory.toServiceIdentifier(serviceIdentifier);
        asserter.isValid(sbd.getAny(), group != null ? new Class<?>[]
                {
                        group
                } : new Class<?>[0]);
    }

    private void validateDpfForsendelseType(StandardBusinessDocument sbd, ServiceRecord serviceRecord) {
        if (svarUtService.getIfAvailable() == null) {
            return;
        }
        if (serviceRecord.getServiceIdentifier() == DPF && sbd.getBusinessMessage() instanceof ArkivmeldingMessage) {
            ArkivmeldingMessage message = (ArkivmeldingMessage) sbd.getBusinessMessage();
            DpfSettings dpfSettings = message.getDpf();
            if (dpfSettings == null) {
                return;
            }
            String forsendelseType = dpfSettings.getForsendelseType();
            if (!isNullOrEmpty(forsendelseType)) {
                List<String> validTypes = svarUtService.getObject().retreiveForsendelseTyper();
                if (!validTypes.contains(forsendelseType)) {
                    throw new ForsendelseTypeNotFoundException(forsendelseType, String.join(",", validTypes));
                }
            }
        }
    }

    @Transactional(noRollbackFor = TimeToLiveException.class)
    public void validate(NextMoveOutMessage message) {
        validateCertificate(message.getServiceIdentifier());

        StandardBusinessDocument sbd = message.getSbd();
        if (SBDUtil.isFileRequired(sbd) && (message.getFiles() == null || message.getFiles().isEmpty())) {
            throw new MissingFileException();
        }

        sbd.getExpectedResponseDateTime().ifPresent(expectedResponseDateTime -> {
            if (sbdService.isExpired(sbd)) {
                timeToLiveHelper.registerErrorStatusAndMessage(sbd, message.getServiceIdentifier(), message.getDirection());
                throw new TimeToLiveException(expectedResponseDateTime);
            }
        });

        if (SBDUtil.isType(message.getSbd(), ARKIVMELDING)) {
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
        if (SBDUtil.isType(message.getSbd(), DIGITAL)) {
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

    private void validateCertificate(ServiceIdentifier si) {
        certificateValidator.ifAvailable(v -> {
            try {
                v.validateCertificate(si);
            } catch (CertificateExpiredException | VirksertCertificateException e) {
                log.error("Certificate validation failed", e);
                throw new InvalidCertificateException(e.getMessage());
            }
        });
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

        // Uncomplete message pre 2.1.1 might have size null.
        // Set to '-1' as workaround as validator accumulates total file size for message.
        message.getFiles().forEach(f -> {
            if (f.getSize() == null) f.setSize(-1L);
        });
        fileSizeValidator.validate(message, file);

        if (message.isPrimaryDocument(file.getOriginalFilename()) && files.stream().anyMatch(BusinessMessageFile::getPrimaryDocument)) {
            throw new MultiplePrimaryDocumentsNotAllowedException();
        }

        if (message.getServiceIdentifier() == DPV && !StringUtils.hasText(file.getName())) {
            throw new MissingFileTitleException(DPV.toString());
        }

        if (message.getServiceIdentifier() == DPI
                && !StringUtils.hasText(file.getName())
                && !message.isPrimaryDocument(file.getOriginalFilename())) {
            message.getBusinessMessage(DpiDigitalMessage.class)
                    .filter(p -> p.getMetadataFiler().containsValue(file.getOriginalFilename()))
                    .orElseThrow(() -> new MissingFileTitleException(DPI.toString()));
        }
    }
}
