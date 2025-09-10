package no.difi.meldingsutveksling.nextmove;

import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.arkivverket.standarder.noark5.metadatakatalog.Journalposttype;
import no.arkivverket.standarder.noark5.metadatakatalog.Journalstatus;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.api.AsicHandler;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.FiksConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.pipe.Reject;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.xml.bind.JAXBException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class SvarInnNextMoveConverterTest {

    @Mock
    private SvarInnService svarInnServiceMock;
    @Mock
    private AsicHandler asicHandlerMock;
    @Mock
    private SBDFactory sbdFactoryMock;
    @Mock
    private IntegrasjonspunktProperties propertiesMock;
    @Mock
    private KeystoreHelper keystoreHelperMock;

    @Mock
    private ArkivmeldingUtil arkivmeldingUtilMock;
    @Mock
    private UUIDGenerator uuidGeneratorMock;

    private final Reject reject = mock(Reject.class);

    @InjectMocks
    private SvarInnNextMoveConverter target;
    private Forsendelse svarInnPackage;

    @BeforeEach
    void setUp() throws JAXBException {
        setupValidSvarInnPackage();
        FiksConfig fiksConfig = mockFiksConfig();
        when(propertiesMock.getFiks()).thenReturn(fiksConfig);
        StandardBusinessDocument standardBusinessDocument = mock(StandardBusinessDocument.class);
        PartnerIdentifier receiverIdentifier = mock(PartnerIdentifier.class);
        when(receiverIdentifier.getPrimaryIdentifier()).thenReturn("receiver-primary-identifier");
        PartnerIdentifier senderIdentifier = mock(PartnerIdentifier.class);
        when(senderIdentifier.hasOrganizationPartIdentifier()).thenReturn(false);
        when(senderIdentifier.getPrimaryIdentifier()).thenReturn("sender-primary-identifier");
        when(standardBusinessDocument.getSenderIdentifier()).thenReturn(senderIdentifier);
        when(standardBusinessDocument.getReceiverIdentifier()).thenReturn(receiverIdentifier);
        when(sbdFactoryMock.createNextMoveSBD(any(PartnerIdentifier.class),
                any(PartnerIdentifier.class),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                any()))
                .thenReturn(standardBusinessDocument);
        when(arkivmeldingUtilMock.marshalArkivmelding(any(Arkivmelding.class))).thenReturn("test".getBytes());
        Resource asicMock = mock(Resource.class);
        when(asicHandlerMock.createCmsEncryptedAsice(
                any(NextMoveMessage.class), any(), any(), any(), any(Reject.class)))
                .thenReturn(asicMock);
        doNothing().when(reject).reject(any(Throwable.class));
    }

    @NotNull
    private static FiksConfig mockFiksConfig() {
        FiksConfig fiksConfig = mock(FiksConfig.class);
        FiksConfig.SvarInn svarInnConfig = mock(FiksConfig.SvarInn.class);
        when(svarInnConfig.getProcess()).thenReturn("svarinn-test-process");
        when(svarInnConfig.getDocumentType()).thenReturn("svarinn-test-dokumenttype");
        when(fiksConfig.getInn()).thenReturn(svarInnConfig);
        return fiksConfig;
    }

    private void setupValidSvarInnPackage() {
        svarInnPackage = new Forsendelse();
        svarInnPackage.setId("Test-forsendelse");
        svarInnPackage.setMetadataFraAvleverendeSystem(getDeliveryData());
        svarInnPackage.setMetadataForImport(getImportMetadata());
        svarInnPackage.setDownloadUrl("https://svarinn.forsendelse.url");
        svarInnPackage.setFilmetadata(Lists.newArrayList());
        svarInnPackage.setSvarSendesTil(getReplyTo());
        svarInnPackage.setSvarPaForsendelse("svarpåforsendelse");
        svarInnPackage.setMottaker(getReceiver());
        svarInnPackage.setTittel("Test-forsendelse-tittel");
    }

    @NotNull
    private static Forsendelse.Mottaker getReceiver() {
        Forsendelse.Mottaker receiver = new Forsendelse.Mottaker();
        receiver.setOrgnr("910568639");
        receiver.setNavn("Sindre Torkelsen");
        return receiver;
    }

    private static Forsendelse.SvarSendesTil getReplyTo() {
        Forsendelse.SvarSendesTil replyTo = new Forsendelse.SvarSendesTil();
        replyTo.setAdresse1("Adresse 1");
        replyTo.setPostnr("1234");
        replyTo.setPoststed("Norge");
        replyTo.setNavn("Sindre Torkelsen");
        replyTo.setLand("Norge");
        replyTo.setOrgnr("910568639");
        replyTo.setFnr("16079415093");
        return replyTo;
    }

    private static Forsendelse.MetadataForImport getImportMetadata() {
        Forsendelse.MetadataForImport metadata = new Forsendelse.MetadataForImport();
        metadata.setSakssekvensnummer(1);
        metadata.setSaksaar(2024);
        metadata.setJournalposttype(Journalposttype.INNGÅENDE_DOKUMENT.value());
        metadata.setJournalstatus(Journalstatus.JOURNALFØRT.value());
        metadata.setDokumentetsDato("1220227200");
        metadata.setTittel("Test-forsendelse-tittel");
        return metadata;
    }

    @NotNull
    private static Forsendelse.MetadataFraAvleverendeSystem getDeliveryData() {
        Forsendelse.MetadataFraAvleverendeSystem metadata = new Forsendelse.MetadataFraAvleverendeSystem();
        metadata.setSakssekvensnummer(1);
        metadata.setSaksaar(2024);
        metadata.setJournalaar("2024");
        metadata.setJournalsekvensnummer("1");
        metadata.setJournalpostnummer("1");
        metadata.setJournalposttype(Journalposttype.INNGÅENDE_DOKUMENT.value());
        metadata.setJournalstatus(Journalstatus.JOURNALFØRT.value());
        metadata.setJournaldato("1220227200");
        metadata.setDokumentetsDato("1220227200");
        metadata.setTittel("Test-forsendelse-tittel");
        metadata.setSaksBehandler("S. Aksbehandler");
        return metadata;
    }

    @Test
    void convert_ValidPackage_ShouldPass() {
        assertDoesNotThrow(() -> target.convert(svarInnPackage, reject));
    }

    @Test
    void convert_JournalaarIsNull_ShouldPass() {
        svarInnPackage.getMetadataFraAvleverendeSystem().setJournalaar(null);
        assertDoesNotThrow(() -> target.convert(svarInnPackage, reject));
    }

    @Test
    void convert_JournalsekvensnummerIsNull_ShouldPass() {
        svarInnPackage.getMetadataFraAvleverendeSystem().setJournalsekvensnummer(null);
        assertDoesNotThrow(() -> target.convert(svarInnPackage, reject));
    }

    @Test
    void convert_JournalpostnummerIsNull_ShouldPass() {
        svarInnPackage.getMetadataFraAvleverendeSystem().setJournalpostnummer(null);
        assertDoesNotThrow(() -> target.convert(svarInnPackage, reject));
    }

}