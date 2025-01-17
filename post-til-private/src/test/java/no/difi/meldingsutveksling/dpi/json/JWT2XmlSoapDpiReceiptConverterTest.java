package no.difi.meldingsutveksling.dpi.json;

import com.nimbusds.jose.Payload;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.*;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Avsender;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Identifikator;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Virksomhetmottaker;
import no.difi.meldingsutveksling.dpi.client.internal.UnpackJWT;
import no.difi.meldingsutveksling.dpi.client.internal.UnpackStandardBusinessDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.xmlunit.builder.Input;
import org.xmlunit.matchers.CompareMatcher;

import java.io.IOException;
import java.time.OffsetDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(classes = JWT2XmlSoapDpiReceiptConverter.class)
class JWT2XmlSoapDpiReceiptConverterTest {

    private static final String JWT = "eyJ4NWMiOlsiTUlJR";
    private static final Payload PAYLOAD = new Payload("payload");
    private static final Iso6523 SENDER = Iso6523.of(ICD.NO_ORG, "111111111");
    private static final Iso6523 RECEIVER = Iso6523.of(ICD.NO_ORG, "222222222");

    @MockBean private UnpackJWT unpackJWT;
    @MockBean private UnpackStandardBusinessDocument unpackStandardBusinessDocument;

    @Autowired
    private JWT2XmlSoapDpiReceiptConverter target;

    @Value("classpath:/xml/feil.xml")
    private Resource feil;

    @Value("classpath:/xml/leveringskvittering.xml")
    private Resource leveringskvittering;

    @Value("classpath:/xml/aapningskvittering.xml")
    private Resource aapningskvittering;

    @Value("classpath:/xml/varslingfeiletkvittering.xml")
    private Resource varslingfeiletkvittering;

    @Value("classpath:/xml/mottakskvittering.xml")
    private Resource mottakskvittering;

    @Value("classpath:/xml/returpostkvittering.xml")
    private Resource returpostkvittering;

    @Test
    void feil() throws IOException {
        runTest(new Feil()
                        .setTidspunkt(OffsetDateTime.parse("2022-04-27T10:10:05.893+00:00"))
                        .setAvsender(new Avsender()
                                .setVirksomhetsidentifikator(new Identifikator()
                                        .setAuthority(SENDER.getAuthority())
                                        .setValue(SENDER.getIdentifier())
                                ))
                        .setMottaker(new Virksomhetmottaker()
                                .setVirksomhetsidentifikator(new Identifikator()
                                        .setAuthority(RECEIVER.getAuthority())
                                        .setValue(RECEIVER.getIdentifier())
                                ))
                        .setFeiltype(Feil.Type.KLIENT)
                        .setDetaljer("Oh no!")
                , DpiMessageType.FEIL, feil);
    }

    @Test
    void leveringskvittering() throws IOException {
        Leveringskvittering kvittering = new Leveringskvittering();
        populate(kvittering);
        runTest(kvittering, DpiMessageType.LEVERINGSKVITTERING, leveringskvittering);
    }

    @Test
    void aapningskvittering() throws IOException {
        Aapningskvittering kvittering = new Aapningskvittering();
        populate(kvittering);
        runTest(kvittering, DpiMessageType.AAPNINGSKVITTERING, aapningskvittering);
    }

    @Test
    void varslingfeiletkvittering() throws IOException {
        Varslingfeiletkvittering kvittering = new Varslingfeiletkvittering()
                .setVarslingskanal(Varslingskanal.SMS)
                .setBeskrivelse("Oh no!");
        populate(kvittering);
        runTest(kvittering, DpiMessageType.VARSLINGFEILETKVITTERING, varslingfeiletkvittering);
    }

    @Test
    void mottakskvittering() throws IOException {
        Mottakskvittering kvittering = new Mottakskvittering();
        populate(kvittering);
        runTest(kvittering, DpiMessageType.MOTTAKSKVITTERING, mottakskvittering);
    }

    @Test
    void returpostkvittering() throws IOException {
        Returpostkvittering kvittering = new Returpostkvittering();
        populate(kvittering);
        runTest(kvittering, DpiMessageType.RETURPOSTKVITTERING, returpostkvittering);
    }

    private void runTest(Object businessMessage, DpiMessageType messageType, Resource expectedResult) throws IOException {
        StandardBusinessDocument sbd = getStandardBusinessDocument(messageType, businessMessage);

        given(unpackJWT.getPayload(any())).willReturn(PAYLOAD);
        given(unpackStandardBusinessDocument.unpackStandardBusinessDocument(any())).willReturn(sbd);

        String result = target.apply(JWT);
        assertThat(result, CompareMatcher.isSimilarTo(Input.fromFile(expectedResult.getFile()))
                .normalizeWhitespace()
        );

        verify(unpackJWT).getPayload(JWT);
        verify(unpackStandardBusinessDocument).unpackStandardBusinessDocument(PAYLOAD);
    }

    private StandardBusinessDocument getStandardBusinessDocument(DpiMessageType messageType, Object businessMessage) {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setHeaderVersion("1.0")
                        .setSenderIdentifier(SENDER)
                        .setReceiverIdentifier(RECEIVER)
                        .setDocumentIdentification(new DocumentIdentification()
                                .setStandard(messageType.getStandard())
                                .setTypeVersion("1.0")
                                .setInstanceIdentifier("b6e442ca-2c62-42f2-abc3-fdbe9c96b9c2")
                                .setType(messageType.getType())
                                .setCreationDateAndTime(OffsetDateTime.parse("2022-04-27T10:10:05.893+00:00")))
                        .setBusinessScope(new BusinessScope()
                                .addScope(new Scope()
                                        .setType(ScopeType.CONVERSATION_ID.getFullname())
                                        .setInstanceIdentifier("6aa00d4b-a9fe-43c1-91ea-88908c118610")
                                        .setIdentifier(messageType.getProcess()))))
                .setAny(businessMessage);
    }

    private void populate(AbstractKvittering kvittering) {
        kvittering.setTidspunkt(OffsetDateTime.parse("2022-04-27T10:10:05.893+00:00"));
        kvittering.setAvsender(new Avsender()
                .setVirksomhetsidentifikator(new Identifikator()
                        .setAuthority(SENDER.getAuthority())
                        .setValue(SENDER.getIdentifier())
                ));
        kvittering.setMottaker(new Virksomhetmottaker()
                .setVirksomhetsidentifikator(new Identifikator()
                        .setAuthority(RECEIVER.getAuthority())
                        .setValue(RECEIVER.getIdentifier())
                ));
    }
}