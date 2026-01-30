package no.difi.meldingsutveksling.altinnv3.dpo;

import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.altinnv3.UseFullTestConfiguration;
import no.difi.meldingsutveksling.altinnv3.dpo.payload.ZipUtils;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.DocumentIdentification;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    AltinnDPOUploadService.class,
    TaskExecutorConfig.class,
    PromiseMaker.class,
    IntegrasjonspunktProperties.class,
    UUIDGenerator.class
})
@UseFullTestConfiguration
public class AltinnUploadServiceTest {

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        // systemowner
        registry.add("difi.move.dpo.systemUser.orgId", () -> "0192:111111111");
        registry.add("difi.move.dpo.systemUser.name", () -> "111111111_integrasjonspunkt_systembruker_test");
        // reportees, på vegne av konfigurasjon
        registry.add("difi.move.dpo.reportees[0].orgId", () -> "0192:222222222");
        registry.add("difi.move.dpo.reportees[0].name", () -> "222222222_integrasjonspunkt_systembruker_test");
    }

    @Autowired
    private AltinnDPOUploadService altinnUploadService;

    @MockitoBean
    private BrokerApiClient brokerApiClient;

    @MockitoBean
    private ZipUtils zipUtils;

    @MockitoBean
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private TransactionStatus transactionStatus;

    private static final Iso6523 SENDER_IS_SYSTEOWNER = Iso6523.of(ICD.NO_ORG, "111111111");
    private static final Iso6523 SENDER_IS_REPORTEE = Iso6523.of(ICD.NO_ORG, "222222222");
    private static final Iso6523 SENDER_IS_UNKNOWN = Iso6523.of(ICD.NO_ORG, "333333333");
    private static final Iso6523 RECEIVER = Iso6523.of(ICD.NO_ORG, "000000000");

    private StandardBusinessDocument sbdFraSystemOwner; //
    private StandardBusinessDocument sbdFraReportee;
    private StandardBusinessDocument sbdFraUkjentAvsender;

    @BeforeEach
    public void beforeEach() {
        InputStreamResource emptyResource = new InputStreamResource(new ByteArrayInputStream(new byte[0]));

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
