package no.difi.meldingsutveksling.altinnv3.dpo;

import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.altinnv3.dpo.payload.ZipUtils;
import no.difi.meldingsutveksling.config.AltinnFormidlingsTjenestenConfig;
import no.difi.meldingsutveksling.config.AltinnSystemUser;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.DocumentIdentification;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.InputStreamResource;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AltinnUploadServiceTest {

    private AltinnDPOUploadService altinnUploadService;

    @Mock
    private BrokerApiClient brokerApiClient;

    @Mock
    private ZipUtils zipUtils;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TransactionStatus transactionStatus;

    @Mock
    private IntegrasjonspunktProperties integrasjonspunktProperties;

    private static final Iso6523 SENDER_IS_SYSTEOWNER = Iso6523.of(ICD.NO_ORG, "111111111");
    private static final Iso6523 SENDER_IS_REPORTEE = Iso6523.of(ICD.NO_ORG, "222222222");
    private static final Iso6523 SENDER_IS_UNKNOWN = Iso6523.of(ICD.NO_ORG, "333333333");
    private static final Iso6523 RECEIVER = Iso6523.of(ICD.NO_ORG, "000000000");

    private StandardBusinessDocument sbdFraSystemOwner; //
    private StandardBusinessDocument sbdFraReportee;
    private StandardBusinessDocument sbdFraUkjentAvsender;

    @BeforeEach
    public void beforeEach() {
        altinnUploadService = new AltinnDPOUploadService(brokerApiClient,
            new PromiseMaker(Runnable::run, transactionTemplate),
            zipUtils,
            integrasjonspunktProperties,
            new UUIDGenerator()
        );

        InputStreamResource emptyResource = new InputStreamResource(new ByteArrayInputStream(new byte[0]));

        var dpoSettings = new AltinnFormidlingsTjenestenConfig();
        dpoSettings.setSystemUser(new AltinnSystemUser().setOrgId("0192:111111111").setName("111111111_integrasjonspunkt_systembruker_test"));
        var reportee = new AltinnSystemUser()
            .setOrgId("0192:222222222")
            .setName("222222222_integrasjonspunkt_systembruker_test");
        var reportees = new HashSet<AltinnSystemUser>();
        reportees.add(reportee);
        dpoSettings.setReportees(reportees);

        when(integrasjonspunktProperties.getDpo()).thenReturn(dpoSettings);
        Mockito.when(zipUtils.getAltinnZip(Mockito.any(), Mockito.any())).thenReturn(emptyResource);
        when(transactionTemplate.execute(any()))
            .thenAnswer(invocation -> invocation.<TransactionCallback<Boolean>>getArgument(0).doInTransaction(transactionStatus));

        sbdFraSystemOwner = new StandardBusinessDocument()
            .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                .setSenderIdentifier(SENDER_IS_SYSTEOWNER)
                .setReceiverIdentifier(RECEIVER)
                .setDocumentIdentification(
                    new DocumentIdentification().setStandard("DummyValue")
                )
            );

        sbdFraReportee = new StandardBusinessDocument()
            .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                .setSenderIdentifier(SENDER_IS_REPORTEE)
                .setReceiverIdentifier(RECEIVER)
                .setDocumentIdentification(
                    new DocumentIdentification().setStandard("DummyValue")
                )
            );

        sbdFraUkjentAvsender = new StandardBusinessDocument()
            .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                .setSenderIdentifier(SENDER_IS_UNKNOWN)
                .setReceiverIdentifier(RECEIVER)
                .setDocumentIdentification(
                    new DocumentIdentification().setStandard("DummyValue")
                )
            );

    }

    @Test
    public void shouldCallZipUtils() {
        altinnUploadService.send(sbdFraSystemOwner);
        verify(zipUtils).getAltinnZip(Mockito.any(), Mockito.any());
    }

    @Test
    public void shouldCallBrokerApiClient_asSystemOwner() {
        altinnUploadService.send(sbdFraSystemOwner);
        verify(brokerApiClient).send(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void shoulCallBrokerApiClient_asReportee() {
        altinnUploadService.send(sbdFraReportee);
        verify(brokerApiClient).send(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void shoulCallBrokerApiClient_asUnknown() {
        var exception = assertThrows(BrokerApiException.class, () ->
            altinnUploadService.send(sbdFraUkjentAvsender)
        );
        assertEquals("Sender 0192:333333333 fra SBD matcher ikke konfigurerte systembrukere", exception.getMessage());
    }

}
