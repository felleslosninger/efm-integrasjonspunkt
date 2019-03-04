package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.config.JacksonConfig;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.nextmove.DpoMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
@ActiveProfiles("test")
@Import(JacksonConfig.class)
public class StandardBusinessDocumentJsonTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private JacksonTester<StandardBusinessDocument> json;

    @Test
    public void testSerialize() throws Exception {
        assertThat(json.write(getDocument())).isEqualToJson("/sbd/StandardBusinessDocument.json");
    }

    @Test
    public void testDeserialize() throws Exception {
        assertThat(json.read("/sbd/StandardBusinessDocument.json")).isEqualTo(getDocument());
    }

    private StandardBusinessDocument getDocument() {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setBusinessScope(new BusinessScope()
                                .addScope(new Scope()
                                        .addScopeInformation(new CorrelationInformation()
                                                .setExpectedResponseDateTime(ZonedDateTime.parse("2003-05-10T00:31:52Z"))
                                        )
                                        .setIdentifier("urn:no:difi:meldingsutveksling:2.0")
                                        .setInstanceIdentifier("37efbd4c-413d-4e2c-bbc5-257ef4a65a45")
                                        .setType("ConversationId")
                                )
                        )
                        .setDocumentIdentification(new DocumentIdentification()
                                .setCreationDateAndTime(ZonedDateTime.parse("2016-04-11T15:29:58.753+02:00"))
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
                )
                .setAny(new DpoMessage()
                        .setDpoField("foo")
                        .setSecurityLevel("3")
                );
    }
}
