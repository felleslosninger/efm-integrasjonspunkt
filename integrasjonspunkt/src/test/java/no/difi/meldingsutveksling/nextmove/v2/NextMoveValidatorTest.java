package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.config.AltinnFormidlingsTjenestenConfig;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDService;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.*;
import no.difi.meldingsutveksling.ks.svarut.SvarUtService;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.meldingsutveksling.validation.Asserter;
import no.difi.meldingsutveksling.validation.IntegrasjonspunktCertificateValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NextMoveValidatorTest {

    @Mock
    private ServiceRecordProvider serviceRecordProvider;
    @Mock
    private NextMoveMessageOutRepository nextMoveMessageOutRepository;
    @Mock
    private ConversationStrategyFactory conversationStrategyFactory;
    @Mock
    private Asserter asserter;
    @Mock
    private OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    @Mock
    private TimeToLiveHelper timeToLiveHelper;
    @Mock
    private SBDService sbdService;
    @Mock
    private ConversationService conversationService;
    @Mock
    private ArkivmeldingUtil arkivmeldingUtil;
    @Mock
    private NextMoveFileSizeValidator nextMoveFileSizeValidator;
    @Mock
    private IntegrasjonspunktProperties props;
    @Mock
    private ObjectProvider<IntegrasjonspunktCertificateValidator> certValidator;
    @Mock
    private SvarUtService svarUtService;
    @Mock
    private ObjectProvider<SvarUtService> svarUtServiceProvider;

    private NextMoveValidator nextMoveValidator;

    private final String messageId = "123";
    @Mock
    private NextMoveOutMessage message;
    @Mock
    private StandardBusinessDocument sbd;
    @Mock
    private ServiceRecord serviceRecord;

    private MockedStatic<SBDUtil> sbdUtilMock;

    private BusinessMessageFile bmf;

    private ArkivmeldingMessage businessMessage;

    @BeforeEach
    void before() {
        nextMoveValidator = new NextMoveValidator(
                serviceRecordProvider,
                nextMoveMessageOutRepository,
                conversationStrategyFactory,
                asserter,
                optionalCryptoMessagePersister,
                timeToLiveHelper,
                sbdService,
                conversationService,
                arkivmeldingUtil,
                nextMoveFileSizeValidator,
                props,
                certValidator,
                svarUtServiceProvider
        );

        bmf = new BusinessMessageFile()
                .setFilename("foo.txt")
                .setPrimaryDocument(true);

        businessMessage = new ArkivmeldingMessage().setHoveddokument("foo.txt");

        sbdUtilMock = mockStatic(SBDUtil.class);
    }

    @AfterEach
    void after() {
        if (sbdUtilMock != null) {
            sbdUtilMock.close();
        }
    }

    @Test
    void message_type_must_fit_document_type() {
        doAnswer(inv -> {
            ((java.util.function.Consumer<IntegrasjonspunktCertificateValidator>) inv.getArgument(0)).accept(mock(IntegrasjonspunktCertificateValidator.class));
            return null;
        }).when(certValidator).ifAvailable(any());
        when(sbd.getMessageId()).thenReturn(messageId);
        when(nextMoveMessageOutRepository.findByMessageId(messageId)).thenReturn(Optional.empty());
        when(conversationService.findConversation(messageId)).thenReturn(Optional.empty());
        when(serviceRecordProvider.getServiceIdentifier(sbd)).thenReturn(ServiceIdentifier.DPO);
        when(conversationStrategyFactory.isEnabled(ServiceIdentifier.DPO)).thenReturn(true);
        sbdUtilMock.when(() -> SBDUtil.isStatus(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isReceipt(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isFileRequired(sbd)).thenReturn(true);
        when(sbd.getDocumentType()).thenReturn("foo::bar");
        when(sbd.getType()).thenReturn("arkivmelding");

        assertThrows(MessageTypeDoesNotFitDocumentTypeException.class, () -> nextMoveValidator.validate(sbd));
    }

    @Test
    void document_type_must_be_valid() {
        when(sbd.getType()).thenReturn("melding");
        assertThrows(UnknownMessageTypeException.class, () -> nextMoveValidator.validate(sbd));
    }

    @Test
    void service_not_enabled_should_throw_exception() {
        doAnswer(inv -> {
            ((java.util.function.Consumer<IntegrasjonspunktCertificateValidator>) inv.getArgument(0)).accept(mock(IntegrasjonspunktCertificateValidator.class));
            return null;
        }).when(certValidator).ifAvailable(any());

        when(sbd.getMessageId()).thenReturn(messageId);
        when(sbd.getType()).thenReturn("arkivmelding");
        when(nextMoveMessageOutRepository.findByMessageId(messageId)).thenReturn(Optional.empty());
        when(conversationService.findConversation(messageId)).thenReturn(Optional.empty());
        when(serviceRecordProvider.getServiceIdentifier(sbd)).thenReturn(ServiceIdentifier.DPO);
        when(conversationStrategyFactory.isEnabled(ServiceIdentifier.DPO)).thenReturn(true);
        sbdUtilMock.when(() -> SBDUtil.isStatus(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isReceipt(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isFileRequired(sbd)).thenReturn(true);
        when(conversationStrategyFactory.isEnabled(ServiceIdentifier.DPO)).thenReturn(false);
        assertThrows(ServiceNotEnabledException.class, () -> nextMoveValidator.validate(sbd));
    }

    @Test
    void duplicate_messageId_not_allowed() {
        when(nextMoveMessageOutRepository.findByMessageId(messageId)).thenReturn(Optional.of(message));
        when(sbd.getMessageId()).thenReturn(messageId);
        sbdUtilMock.when(() -> SBDUtil.isStatus(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isReceipt(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isFileRequired(sbd)).thenReturn(true);
        assertThrows(MessageAlreadyExistsException.class, () -> nextMoveValidator.validate(sbd));
    }

    @Test
    void conversation_cannot_exist_with_same_messageId() {
        when(sbd.getMessageId()).thenReturn(messageId);
        when(nextMoveMessageOutRepository.findByMessageId(messageId)).thenReturn(Optional.empty());
        when(conversationService.findConversation(messageId)).thenReturn(Optional.empty());
        sbdUtilMock.when(() -> SBDUtil.isStatus(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isReceipt(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isFileRequired(sbd)).thenReturn(true);
        when(conversationService.findConversation(messageId)).thenReturn(Optional.of(mock(Conversation.class)));

        assertThrows(MessageAlreadyExistsException.class, () -> nextMoveValidator.validate(sbd));
    }

    @Test
    void non_receipt_messages_must_have_attachments() {
        doAnswer(inv -> {
            ((java.util.function.Consumer<IntegrasjonspunktCertificateValidator>) inv.getArgument(0)).accept(mock(IntegrasjonspunktCertificateValidator.class));
            return null;
        }).when(certValidator).ifAvailable(any());
        when(message.getSbd()).thenReturn(sbd);
        when(message.getFiles()).thenReturn(java.util.Collections.emptySet());
        sbdUtilMock.when(() -> SBDUtil.isStatus(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isReceipt(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isFileRequired(sbd)).thenReturn(true);
        when(message.getFiles()).thenReturn(null);

        assertThrows(MissingFileException.class, () -> nextMoveValidator.validate(message));
    }

    @Test
    void duplicate_filenames_not_allowed() {
        when(message.getOrCreateFiles()).thenReturn(new java.util.LinkedHashSet<>(java.util.Set.of(bmf)));
        sbdUtilMock.when(() -> SBDUtil.isStatus(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isReceipt(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isFileRequired(sbd)).thenReturn(true);

        BasicNextMoveFile file = BasicNextMoveFile.of("title", "foo.txt", "text", "foo".getBytes());
        assertThrows(DuplicateFilenameException.class, () -> nextMoveValidator.validateFile(message, file));
    }

    @Test
    void unknown_document_type_allowed_for_fiksio_message() {
        when(sbd.getDocumentType()).thenReturn("foo::bar");
        when(sbd.getType()).thenReturn("fiksio");
        when(serviceRecordProvider.getServiceIdentifier(any())).thenReturn(ServiceIdentifier.DPFIO);
        when(conversationStrategyFactory.isEnabled(ServiceIdentifier.DPFIO)).thenReturn(true);

        nextMoveValidator.validate(sbd);
    }

    @Test
    void dpo_message_does_not_require_title() {
        String filename = "bar.txt";
        when(message.isPrimaryDocument(filename)).thenReturn(false);
        BasicNextMoveFile file = BasicNextMoveFile.of("", filename, "text", "foo".getBytes());
        nextMoveValidator.validateFile(message, file);
    }

    @Test
    void dpv_message_requires_title() {
        String filename = "bar.txt";
        when(message.getServiceIdentifier()).thenReturn(ServiceIdentifier.DPV);
        when(message.isPrimaryDocument(filename)).thenReturn(false);
        BasicNextMoveFile file = BasicNextMoveFile.of("", "bar.txt", "text", "foo".getBytes());
        assertThrows(MissingFileTitleException.class, () -> nextMoveValidator.validateFile(message, file));
    }

    @Test
    void non_matching_channel_should_throw_exception() {
        AltinnFormidlingsTjenestenConfig dpo = mock(AltinnFormidlingsTjenestenConfig.class);
        when(props.getDpo()).thenReturn(dpo);
        when(dpo.getMessageChannel()).thenReturn("foo-42");

        when(sbd.getType()).thenReturn("arkivmelding");
        sbdUtilMock.when(() -> SBDUtil.getOptionalMessageChannel(sbd)).thenReturn(
                Optional.of(new no.difi.meldingsutveksling.domain.sbdh.Scope().setIdentifier("foo-43"))
        );

        doAnswer(inv -> {
            ((java.util.function.Consumer<IntegrasjonspunktCertificateValidator>) inv.getArgument(0)).accept(mock(IntegrasjonspunktCertificateValidator.class));
            return null;
        }).when(certValidator).ifAvailable(any());

        when(sbd.getMessageId()).thenReturn(messageId);
        when(sbd.getDocumentType()).thenReturn("standard::arkivmelding");
        when(nextMoveMessageOutRepository.findByMessageId(messageId)).thenReturn(Optional.empty());
        when(conversationService.findConversation(messageId)).thenReturn(Optional.empty());
        when(serviceRecordProvider.getServiceIdentifier(sbd)).thenReturn(ServiceIdentifier.DPO);
        when(conversationStrategyFactory.isEnabled(ServiceIdentifier.DPO)).thenReturn(true);
        sbdUtilMock.when(() -> SBDUtil.isStatus(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isReceipt(sbd)).thenReturn(false);
        sbdUtilMock.when(() -> SBDUtil.isFileRequired(sbd)).thenReturn(true);

        assertThrows(MessageChannelInvalidException.class, () -> nextMoveValidator.validate(sbd));
    }
}
