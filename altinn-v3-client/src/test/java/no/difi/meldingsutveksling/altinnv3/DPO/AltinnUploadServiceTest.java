package no.difi.meldingsutveksling.altinnv3.DPO;

import io.netty.util.concurrent.Promise;
import jakarta.inject.Inject;
import no.difi.meldingsutveksling.altinnv3.AltinnConfig;
import no.difi.meldingsutveksling.altinnv3.AltinnTokenUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.move.common.io.pipe.Plumber;
import no.difi.move.common.io.pipe.PromiseMaker;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;

@Disabled
@SpringBootTest(classes = {
    AltinnConfig.class,
    AltinnUploadService.class,
    BrokerApiClient.class,
    AltinnTokenUtil.class,
    IntegrasjonspunktProperties.class,
    PromiseMaker.class,
    TaskExecutor.class,
    TaskExecutorConfig.class,
    Plumber.class,
})
@ConfigurationPropertiesScan
public class AltinnUploadServiceTest {
    @Autowired
    private AltinnUploadService service;
    @MockitoBean
    private TransactionTemplate transactionTemplate; // todo not mock or handle mock?

    private static final Iso6523 SENDER = Iso6523.of(ICD.NO_ORG, "111111111");
    private static final Iso6523 RECEIVER = Iso6523.of(ICD.NO_ORG, "222222222");

    @Test
    public void upload() {
        StandardBusinessDocument sbd = new StandardBusinessDocument()
            .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                .setHeaderVersion("1.0")
                .setSenderIdentifier(SENDER)
                .setReceiverIdentifier(RECEIVER)
                .setDocumentIdentification(new DocumentIdentification()
//                    .setStandard(messageType.getStandard())
                    .setTypeVersion("1.0")
                    .setInstanceIdentifier("b6e442ca-2c62-42f2-abc3-fdbe9c96b9c2")
//                    .setType(messageType.getType())
                    .setCreationDateAndTime(OffsetDateTime.parse("2022-04-27T10:10:05.893+00:00")))
                .setBusinessScope(new BusinessScope()
                    .addScope(new Scope()
                        .setType(ScopeType.CONVERSATION_ID.getFullname())
                        .setInstanceIdentifier("6aa00d4b-a9fe-43c1-91ea-88908c118610")
//                        .setIdentifier(messageType.getProcess()))))
                    )));
//            .setAny(businessMessage);

        service.send(sbd);
    }
}
