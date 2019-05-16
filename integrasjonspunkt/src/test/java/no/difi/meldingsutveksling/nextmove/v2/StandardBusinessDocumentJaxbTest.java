package no.difi.meldingsutveksling.nextmove.v2;

import lombok.SneakyThrows;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import org.junit.Before;
import org.junit.Test;
import org.springframework.xml.transform.StringResult;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

public class StandardBusinessDocumentJaxbTest {

    private Marshaller marshaller;
    private Unmarshaller unmarshaller;
    private ObjectFactory objectFactory;

    @Before
    @SneakyThrows
    public void createMarshaller() {
        JAXBContext context = JAXBContext.newInstance(StandardBusinessDocument.class, ArkivmeldingMessage.class);
        marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        unmarshaller = context.createUnmarshaller();
        objectFactory = new ObjectFactory();
    }

    @Test
    @SneakyThrows
    public void testMarshall() {
        StringResult result = new StringResult();
        marshaller.marshal(objectFactory.createStandardBusinessDocument(getDocument()), result);
        assertThat(result.toString()).isXmlEqualTo(
                contentOf(getClass().getResource("/sbd/StandardBusinessDocument.xml"))
        );
    }

    @Test
    @SneakyThrows
    public void testUnmarshall() {
        StandardBusinessDocument document = unmarshaller.unmarshal(new StreamSource(
                        getClass().getResourceAsStream("/sbd/StandardBusinessDocument.xml")),
                StandardBusinessDocument.class).getValue();
        assertThat(document.toString()).isEqualTo(getDocument().toString());
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
                                .setType("DPO")
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
                .setAny(new ArkivmeldingMessage()
                        .setSikkerhetsnivaa(3));
    }
}
