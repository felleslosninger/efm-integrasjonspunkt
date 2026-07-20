package no.difi.meldingsutveksling.nextmove.v2;

import com.google.common.base.Strings;
import jakarta.validation.groups.Default;
import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.DuplicateFilenameException;
import no.difi.meldingsutveksling.exceptions.FileNotFoundException;
import no.difi.meldingsutveksling.exceptions.ForsendelseTypeNotFoundException;
import no.difi.meldingsutveksling.exceptions.InvalidCertificateException;
import no.difi.meldingsutveksling.exceptions.InvalidContentTypeException;
import no.difi.meldingsutveksling.exceptions.InvalidDocumentException;
import no.difi.meldingsutveksling.exceptions.MessageAlreadyExistsException;
import no.difi.meldingsutveksling.exceptions.MessageChannelInvalidException;
import no.difi.meldingsutveksling.exceptions.MessageTypeDoesNotFitDocumentTypeException;
import no.difi.meldingsutveksling.exceptions.MissingAddressInformationException;
import no.difi.meldingsutveksling.exceptions.MissingArkivmeldingException;
import no.difi.meldingsutveksling.exceptions.MissingArkivmeldingFileException;
import no.difi.meldingsutveksling.exceptions.MissingFileException;
import no.difi.meldingsutveksling.exceptions.MissingFileTitleException;
import no.difi.meldingsutveksling.exceptions.MissingPrimaryDocumentException;
import no.difi.meldingsutveksling.exceptions.MissingSvarUtCredentialsException;
import no.difi.meldingsutveksling.exceptions.MultiplePrimaryDocumentsNotAllowedException;
import no.difi.meldingsutveksling.exceptions.ReceiverException;
import no.difi.meldingsutveksling.exceptions.SenderException;
import no.difi.meldingsutveksling.exceptions.ServiceNotEnabledException;
import no.difi.meldingsutveksling.exceptions.TimeToLiveException;
import no.difi.meldingsutveksling.exceptions.UnknownMessageTypeException;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFile;
import no.difi.meldingsutveksling.nextmove.ConversationStrategyFactory;
import no.difi.meldingsutveksling.nextmove.DialogmeldingMessage;
import no.difi.meldingsutveksling.nextmove.DpfSettings;
import no.difi.meldingsutveksling.nextmove.DpiDigitalMessage;
import no.difi.meldingsutveksling.nextmove.DpiPrintMessage;
import no.difi.meldingsutveksling.nextmove.MailReturn;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.meldingsutveksling.nextmove.TimeToLiveHelper;
import no.difi.meldingsutveksling.nhn.adapter.model.AttachmentNames;
import no.difi.meldingsutveksling.nhn.adapter.model.serialization.ApplicationReceiptDeserializer;
import no.difi.meldingsutveksling.nhn.adapter.model.serialization.ApplicationReceiptException;
import no.difi.meldingsutveksling.nhn.adapter.model.serialization.DialogmeldingDeserializer;
import no.difi.meldingsutveksling.validation.Asserter;
import no.difi.meldingsutveksling.validation.IntegrasjonspunktCertificateValidator;
import no.difi.meldingsutveksling.validation.VirksertCertificateException;
import no.difi.meldingsutveksling.validation.group.ValidationGroupFactory;
import no.difi.move.common.io.ResourceUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateExpiredException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.MessageType.ARKIVMELDING;
import static no.difi.meldingsutveksling.MessageType.DIALOGMELDING;
import static no.difi.meldingsutveksling.MessageType.DIGITAL;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPF;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPH;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;
import static no.difi.meldingsutveksling.domain.PartnerUtil.getPartOrPrimaryIdentifier;
import static no.difi.meldingsutveksling.domain.sbdh.Authority.NHN_ACTORID;


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

        messageRepo.findByMessageId(sbd.getMessageId())
            .ifPresent(p -> {
                throw new MessageAlreadyExistsException(sbd.getMessageId());
            });
        if (!SBDUtil.isStatus(sbd)) {
            conversationService.findConversation(sbd.getMessageId())
                .ifPresent(c -> {
                    throw new MessageAlreadyExistsException(sbd.getMessageId());
                });
        }

        validateCertificate();

        MessageType messageType = MessageType.valueOfType(sbd.getType())
            .orElseThrow(() -> new UnknownMessageTypeException(sbd.getType()));

        ServiceIdentifier serviceIdentifier = serviceRecordProvider.getServiceIdentifier(sbd);

        Class<?> serviceIdentifierGroup = ValidationGroupFactory.toServiceIdentifier(serviceIdentifier);
        asserter.isValid(sbd, serviceIdentifierGroup);
        Class<?> documentTypeGroup = ValidationGroupFactory.toDocumentType(messageType);
        if (documentTypeGroup != null) {
            asserter.isValid(sbd, documentTypeGroup);
        }

        if (!conversationStrategyFactory.isEnabled(serviceIdentifier)) {
            throw new ServiceNotEnabledException(serviceIdentifier);
        }

        if (messageType == MessageType.PRINT) {
            validatePrintBusinessMessage(sbd);
        }

        if (!messageType.fitsDocumentIdentifier(sbd.getDocumentType())) {
            throw new MessageTypeDoesNotFitDocumentTypeException(messageType, sbd.getDocumentType());
        }

        if (serviceIdentifier == DPO && !isNullOrEmpty(props.getDpo().getMessageChannel())) {
            Optional<Scope> mc = SBDUtil.getOptionalMessageChannel(sbd);
            if (mc.isPresent() && !mc.get().getIdentifier().equals(props.getDpo().getMessageChannel())) {
                throw new MessageChannelInvalidException(props.getDpo().getMessageChannel(), mc.get().getIdentifier());
            }
        }

        validateDpfForsendelse(sbd, serviceIdentifier);

        if (serviceIdentifier == DPH) {
            asserter.isValid(sbd.getAny(), Default.class);

            if (sbd.getSenderIdentifier() instanceof NhnIdentifier nhnIdentifier) {
                if (!props.getDph().getHerIds().contains(nhnIdentifier.getHerId())) {
                    throw new SenderException("Sender have to be one of the her-ids listed in the difi.move.dph.her-ids property!");
                }
            } else {
                throw new SenderException("Sender have to be a %s for DPH messages!".formatted(NHN_ACTORID));
            }

            if (!(sbd.getReceiverIdentifier() instanceof NhnIdentifier)) {
                throw new ReceiverException("Receiver have to be a %s for DPH messages!".formatted(NHN_ACTORID));
            }
        }
    }

    private static void validatePrintBusinessMessage(StandardBusinessDocument sbd) {
        DpiPrintMessage businessMessage = (DpiPrintMessage) sbd.getAny();
        PostAddress receiverAddress = businessMessage.getMottaker();
        if (receiverAddress == null) {
            throw new MissingAddressInformationException("mottaker");
        }
        boolean receiverIsNorwegian = isNorwegian(receiverAddress);
        if (Strings.isNullOrEmpty(receiverAddress.getAdresselinje1())
            || receiverIsNorwegian && Strings.isNullOrEmpty(receiverAddress.getPostnummer())
            || receiverIsNorwegian && Strings.isNullOrEmpty(receiverAddress.getPoststed())) {
            throw new MissingAddressInformationException("mottaker.postnummer/poststed/adresselinje1");
        }
        MailReturn returnAddress = businessMessage.getRetur();
        if (returnAddress == null) {
            throw new MissingAddressInformationException("retur");
        }
        PostAddress returnReceiver = returnAddress.getMottaker();
        boolean returnToNorway = isNorwegian(returnReceiver);
        if (Strings.isNullOrEmpty(returnReceiver.getAdresselinje1())
            || returnToNorway && Strings.isNullOrEmpty(returnReceiver.getPostnummer())
            || returnToNorway && Strings.isNullOrEmpty(returnReceiver.getPoststed())) {
            throw new MissingAddressInformationException("retur.postnummer/poststed/adresselinje1");
        }
    }

    private static boolean isNorwegian(PostAddress address) {
        String country = address.getLand();
        if (null == country) {
            return true;
        }
        return StringUtils.hasText(country)
            && country.equalsIgnoreCase("Norge")
            || country.equalsIgnoreCase("Norway");
    }

    private void validateDpfForsendelse(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier) {
        if (svarUtService.getIfAvailable() == null) {
            return;
        }
        if (serviceIdentifier == DPF) {
            sbd.getBusinessMessage(ArkivmeldingMessage.class).ifPresent(message -> {
                DpfSettings dpfSettings = message.getDpf();
                if (dpfSettings == null) {
                    return;
                }
                String forsendelseType = dpfSettings.getForsendelseType();
                if (!isNullOrEmpty(forsendelseType)) {
                    List<String> validTypes = svarUtService.getObject()
                        .retreiveForsendelseTyper(getPartOrPrimaryIdentifier(sbd.getSenderIdentifier()));
                    if (!validTypes.contains(forsendelseType)) {
                        throw new ForsendelseTypeNotFoundException(forsendelseType, String.join(",", validTypes));
                    }
                }
            });

            String senderIdentifier = getPartOrPrimaryIdentifier(sbd.getSenderIdentifier());
            if (senderIdentifier.equals(props.getOrg().getNumber())) {
                if (isNullOrEmpty(props.getFiks().getUt().getUsername())) {
                    throw new MissingSvarUtCredentialsException(senderIdentifier);
                }
            } else {
                if (!props.getFiks().getUt().getPaaVegneAv().containsKey(senderIdentifier)) {
                    throw new MissingSvarUtCredentialsException(senderIdentifier);
                }
            }
        }
    }

    @Transactional(noRollbackFor = TimeToLiveException.class)
    public void validate(NextMoveOutMessage message) {
        validateCertificate();

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

        if ((message.getServiceIdentifier() == DPI || (message.getServiceIdentifier() == DPH && SBDUtil.isType(message.getSbd(), DIALOGMELDING))) && message.getFiles().stream()
            .noneMatch(BusinessMessageFile::getPrimaryDocument)) {
            throw new MissingPrimaryDocumentException();
        }
    }

    private void validateCertificate() {
        certificateValidator.ifAvailable(v -> {
            try {
                v.validateCertificate();
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

        try {
            Resource resource = optionalCryptoMessagePersister.read(message.getMessageId(), arkivmeldingFile.getIdentifier());
            return arkivmeldingUtil.unmarshalArkivmelding(resource);
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

        // Uncompleted message pre 2.1.1 might have size null.
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

        if (message.getServiceIdentifier() == DPH && !message.isPrimaryDocument(file.getOriginalFilename())) {
            message.getBusinessMessage(DialogmeldingMessage.class)
                .filter(p -> p.getMetadataFiler().containsKey(file.getOriginalFilename()))
                .orElseThrow(() -> new MissingFileTitleException(DPH.toString()));

            if (!DialogmeldingMessage.MIME_TYPES.contains(file.getContentType())) {
                throw new InvalidContentTypeException("Invalid content type: " + file.getContentType());
            }

            try {
                if (AttachmentNames.DIALOGMELDING.equals(file.getOriginalFilename())) {
                    String xml = ResourceUtils.toString(file.getResource(), StandardCharsets.UTF_8);
                    DialogmeldingDeserializer.deserializeDialogmelding(xml);
                } else if (AttachmentNames.KVITTERING.equals(file.getOriginalFilename())) {
                    String xml = ResourceUtils.toString(file.getResource(), StandardCharsets.UTF_8);
                    ApplicationReceiptDeserializer.deserializeAppRec(xml);
                }
            } catch (ApplicationReceiptException ex) {
                throw new InvalidDocumentException(ex.getMessage());
            }
        }
    }
}
