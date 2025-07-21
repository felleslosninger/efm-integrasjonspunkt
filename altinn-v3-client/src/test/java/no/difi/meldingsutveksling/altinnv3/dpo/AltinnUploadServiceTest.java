package no.difi.meldingsutveksling.altinnv3.dpo;

import no.difi.meldingsutveksling.altinnv3.dpo.payload.ZipUtils;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    AltinnDPOUploadService.class,
    TaskExecutorConfig.class,
    PromiseMaker.class,
    Plumber.class,
    IntegrasjonspunktProperties.class,
})
@ConfigurationPropertiesScan
public class AltinnUploadServiceTest {

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

    private static final Iso6523 SENDER = Iso6523.of(ICD.NO_ORG, "111111111");
    private static final Iso6523 RECEIVER = Iso6523.of(ICD.NO_ORG, "222222222");
    private StandardBusinessDocument sbd;

    @BeforeEach
    public void beforeEach() {
        InputStreamResource emptyResource = new InputStreamResource(new ByteArrayInputStream(new byte[0]));

        Mockito.when(zipUtils.getAltinnZip(Mockito.any(), Mockito.any())).thenReturn(emptyResource);
        when(transactionTemplate.execute(any()))
            .thenAnswer(invocation -> invocation.<TransactionCallback<Boolean>>getArgument(0).doInTransaction(transactionStatus));

        sbd = new StandardBusinessDocument()
            .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                .setSenderIdentifier(SENDER)
                .setReceiverIdentifier(RECEIVER)
            );
    }

    @Test
    public void shouldCallZipUtils() {
        altinnUploadService.send(sbd);

        verify(zipUtils).getAltinnZip(Mockito.any(), Mockito.any());
    }

    @Test
    public void shouldCallBrokerApiClient() {
        altinnUploadService.send(sbd);

        verify(brokerApiClient).send(Mockito.any(), Mockito.any());
    }
}
