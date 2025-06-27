package no.difi.meldingsutveksling.altinnv3.DPO;

import no.difi.meldingsutveksling.altinnv3.DPO.altinn2.ZipHelper;
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
    AltinnUploadService.class,
    TaskExecutorConfig.class,
    PromiseMaker.class,
    Plumber.class
})
public class AltinnUploadServiceTest {

    @MockitoBean
    private BrokerApiClient brokerApiClient;

    @MockitoBean
    private IntegrasjonspunktProperties integrasjonspunktProperties;

    @MockitoBean
    private ZipHelper zipHelper;

    @MockitoBean private TransactionTemplate transactionTemplate;

    @MockitoBean
    private TransactionStatus transactionStatus;

    @Autowired
    private AltinnUploadService altinnUploadService;

    private static final Iso6523 SENDER = Iso6523.of(ICD.NO_ORG, "111111111");
    private static final Iso6523 RECEIVER = Iso6523.of(ICD.NO_ORG, "222222222");

    @BeforeEach
    public void beforeEach() {
        when(transactionTemplate.execute(any()))
            .thenAnswer(invocation -> invocation.<TransactionCallback<Boolean>>getArgument(0).doInTransaction(transactionStatus));
    }

    @Test
    public void upload() { // todo rename
        StandardBusinessDocument sbd = new StandardBusinessDocument()
            .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                .setSenderIdentifier(SENDER)
                .setReceiverIdentifier(RECEIVER)
            );

        InputStreamResource emptyResource = new InputStreamResource(new ByteArrayInputStream(new byte[0]));

        Mockito.when(zipHelper.getAltinnZip(Mockito.any(), Mockito.any())).thenReturn(emptyResource);

        altinnUploadService.send(sbd);

        verify(zipHelper).getAltinnZip(Mockito.any(), Mockito.any());
        verify(brokerApiClient).send(Mockito.any(), Mockito.any());
    }
}
