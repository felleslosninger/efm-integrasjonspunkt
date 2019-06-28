package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class StandardBusinessDocumentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StandardBusinessDocumentRepository repository;

    @Test
    public void test() {
        StandardBusinessDocument document = getDocument();
        entityManager.getEntityManager().persist(document);
        entityManager.flush();
        entityManager.clear();

        StandardBusinessDocument result = repository.findById(document.getId())
                .orElseThrow(() -> new IllegalArgumentException("Entity not found!"));

        assertThat(result).isNotNull();
    }

    private StandardBusinessDocument getDocument() {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setBusinessScope(new BusinessScope()
                                .addScope(new Scope()
                                        .addScopeInformation(new CorrelationInformation()
                                                .setExpectedResponseDateTime(OffsetDateTime.parse("2003-05-10T00:31:52Z"))
                                        )
                                        .setIdentifier("urn:no:difi:meldingsutveksling:2.0")
                                        .setInstanceIdentifier("37efbd4c-413d-4e2c-bbc5-257ef4a65a45")
                                        .setType("ConversationId")
                                )
                        )
                        .setDocumentIdentification(new DocumentIdentification()
                                .setCreationDateAndTime(OffsetDateTime.parse("2016-04-11T15:29:58.753+02:00"))
                                .setInstanceIdentifier("ff88849c-e281-4809-8555-7cd54952b916")
                                .setStandard("urn:no:difi:meldingsutveksling:2.0")
                                .setType("digitaPost/noark5json")
                                .setTypeVersion("2.0")
                        )
                        .setHeaderVersion("1.0")
                        .addReceiver(new Receiver()
                                .setIdentifier(new PartnerIdentification()
                                        .setAuthority("iso6523-actorid-upis")
                                        .setValue("9908:910075918")
                                )
                        )
                        .addSender(new Sender()
                                .setIdentifier(new PartnerIdentification()
                                        .setAuthority("iso6523-actorid-upis")
                                        .setValue("9908:910077473")
                                )
                        )
                ).setAny(new ArkivmeldingMessage()
                        .setSikkerhetsnivaa(3)
                );
    }
}

