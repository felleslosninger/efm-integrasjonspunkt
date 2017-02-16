package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.MessageSender;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * tests for the EduMessageStrategyFactory
 *
 * @author Glenn Bech
 */
public class EduMessageStrategyFactoryTest {

    private String p360Message = "<s:Envelope encoding=\"utf-8\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"><PutMessageRequest xmlns=\"http://www.arkivverket.no/Noark/Exchange/types\"><envelope contentNamespace=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\" conversationId=\"1ca54c1c-74ab-44b4-b3af-6f92a59b2f67\" xmlns=\"\"><sender><orgnr/></sender><receiver><orgnr>974720760</orgnr></receiver></envelope><payload xsi:type=\"xsd:string\" xmlns=\"\">&lt;?xml version=\"1.0\" encoding=\"utf-8\"?&gt;\n" +
            "&lt;Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"&gt;\n" +
            "  &lt;journpost xmlns=\"\"&gt;\n" +
            "    &lt;jpId&gt;210707&lt;/jpId&gt;\n" +
            "    &lt;jpJaar&gt;2015&lt;/jpJaar&gt;\n" +
            "    &lt;jpSeknr&gt;47&lt;/jpSeknr&gt;\n" +
            "    &lt;jpJpostnr&gt;7&lt;/jpJpostnr&gt;\n" +
            "    &lt;jpJdato&gt;0001-01-01&lt;/jpJdato&gt;\n" +
            "    &lt;jpNdoktype&gt;U&lt;/jpNdoktype&gt;\n" +
            "    &lt;jpDokdato&gt;2015-09-11&lt;/jpDokdato&gt;\n" +
            "    &lt;jpStatus&gt;R&lt;/jpStatus&gt;\n" +
            "    &lt;jpInnhold&gt;Testdokument 7&lt;/jpInnhold&gt;\n" +
            "    &lt;jpForfdato /&gt;\n" +
            "    &lt;jpTgkode&gt;U&lt;/jpTgkode&gt;\n" +
            "    &lt;jpAgdato /&gt;\n" +
            "    &lt;jpAntved /&gt;\n" +
            "    &lt;jpSaar&gt;2015&lt;/jpSaar&gt;\n" +
            "    &lt;jpSaseknr&gt;20&lt;/jpSaseknr&gt;\n" +
            "    &lt;jpOffinnhold&gt;Testdokument 7&lt;/jpOffinnhold&gt;\n" +
            "    &lt;jpTggruppnavn&gt;Alle&lt;/jpTggruppnavn&gt;\n" +
            "    &lt;avsmot&gt;\n" +
            "      &lt;amIhtype&gt;0&lt;/amIhtype&gt;\n" +
            "      &lt;amNavn&gt;Saksbehandler Testbruker7&lt;/amNavn&gt;\n" +
            "      &lt;amAdresse&gt;Postboks 8115 Dep.&lt;/amAdresse&gt;\n" +
            "      &lt;amPostnr&gt;0032&lt;/amPostnr&gt;\n" +
            "      &lt;amPoststed&gt;OSLO&lt;/amPoststed&gt;\n" +
            "      &lt;amUtland&gt;Norge&lt;/amUtland&gt;\n" +
            "      &lt;amEpostadr&gt;sa-user.test2@difi.no&lt;/amEpostadr&gt;\n" +
            "    &lt;/avsmot&gt;\n" +
            "    &lt;avsmot&gt;\n" +
            "      &lt;amOrgnr&gt;974720760&lt;/amOrgnr&gt;\n" +
            "      &lt;amIhtype&gt;1&lt;/amIhtype&gt;\n" +
            "      &lt;amNavn&gt;EduTestOrg 1&lt;/amNavn&gt;\n" +
            "    &lt;/avsmot&gt;\n" +
            "    &lt;dokument&gt;\n" +
            "      &lt;dlRnr&gt;1&lt;/dlRnr&gt;\n" +
            "      &lt;dlType&gt;H&lt;/dlType&gt;\n" +
            "      &lt;dbTittel&gt;Testdokument 7&lt;/dbTittel&gt;\n" +
            "      &lt;dbStatus&gt;B&lt;/dbStatus&gt;\n" +
            "      &lt;veVariant&gt;P&lt;/veVariant&gt;\n" +
            "      &lt;veDokformat&gt;DOCX&lt;/veDokformat&gt;\n" +
            "      &lt;veFilnavn&gt;Testdokument 7.DOCX&lt;/veFilnavn&gt;\n" +
            "      &lt;veMimeType&gt;application/vnd.openxmlformats-officedocument.wordprocessingml.document&lt;/veMimeType&gt;\n" +
            "    &lt;/dokument&gt;\n" +
            "  &lt;/journpost&gt;\n" +
            "  &lt;noarksak xmlns=\"\"&gt;\n" +
            "    &lt;saId&gt;15/00020&lt;/saId&gt;\n" +
            "    &lt;saSaar&gt;2015&lt;/saSaar&gt;\n" +
            "    &lt;saSeknr&gt;20&lt;/saSeknr&gt;\n" +
            "    &lt;saPapir&gt;0&lt;/saPapir&gt;\n" +
            "    &lt;saDato&gt;2015-09-01&lt;/saDato&gt;\n" +
            "    &lt;saTittel&gt;BEST/EDU testsak&lt;/saTittel&gt;\n" +
            "    &lt;saStatus&gt;R&lt;/saStatus&gt;\n" +
            "    &lt;saArkdel&gt;Sakarkiv 2013&lt;/saArkdel&gt;\n" +
            "    &lt;saType&gt;Sak&lt;/saType&gt;\n" +
            "    &lt;saJenhet&gt;Oslo&lt;/saJenhet&gt;\n" +
            "    &lt;saTgkode&gt;U&lt;/saTgkode&gt;\n" +
            "    &lt;saBevtid /&gt;\n" +
            "    &lt;saKasskode&gt;B&lt;/saKasskode&gt;\n" +
            "    &lt;saOfftittel&gt;BEST/EDU testsak&lt;/saOfftittel&gt;\n" +
            "    &lt;saAdmkort&gt;202286&lt;/saAdmkort&gt;\n" +
            "    &lt;saAdmbet&gt;Seksjon for test 1&lt;/saAdmbet&gt;\n" +
            "    &lt;saAnsvinit&gt;difi\\sa-user-test2&lt;/saAnsvinit&gt;\n" +
            "    &lt;saAnsvnavn&gt;Saksbehandler Testbruker7&lt;/saAnsvnavn&gt;\n" +
            "    &lt;saTggruppnavn&gt;Alle&lt;/saTggruppnavn&gt;\n" +
            "    &lt;sakspart&gt;\n" +
            "      &lt;spId&gt;0&lt;/spId&gt;\n" +
            "    &lt;/sakspart&gt;\n" +
            "  &lt;/noarksak&gt;\n" +
            "&lt;/Melding&gt;</payload></PutMessageRequest></s:Body></s:Envelope>";


    private EduMessageStrategyFactory eduMessageStrategyFactory;

    private String appReceiptPayload = "lt;AppReceipt type=\"OK\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark/Exchange/types\"&gt;\n" +
            "    &lt;message code=\"ID\" xmlns=\"\"&gt;\n" +
            "    &lt;text&gt;210725&lt;/text&gt;\n" +
            "    &lt;/message&gt;\n" +
            "    &lt;/AppReceipt&gt;";

    private MessageSender messageSender;
    private IntegrasjonspunktProperties properties;

    @Before
    public void createContxt() {
        properties = mock(IntegrasjonspunktProperties.class);
        messageSender = mock(MessageSender.class);
        eduMessageStrategyFactory = EduMessageStrategyFactory.newInstance(messageSender, properties);
    }

    @Test
    public void testShouldCreateBestEDUStrategy() {
        assertEquals(BestEDUMessageStrategy.class, eduMessageStrategyFactory.create(p360Message).getClass());
    }

    @Test
    public void testShouldCreateBestEDUStrategyForEhporteMessage() throws ParserConfigurationException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();
        Element element = doc.createElementNS("http://www.arkivverket.no/Noark4-1-WS-WD/types", "Melding");
        assertEquals(BestEDUMessageStrategy.class, eduMessageStrategyFactory.create(element).getClass());
    }

    @Test
    public void testShouldCreateAppReceiptStrategy() {
        assertTrue(eduMessageStrategyFactory.newInstance(messageSender, properties).create(appReceiptPayload) instanceof AppReceiptMessageStrategy);
        assertEquals(AppReceiptMessageStrategy.class, eduMessageStrategyFactory.create(appReceiptPayload).getClass());
    }

    @Test(expected = MeldingsUtvekslingRuntimeException.class)
    public void testShoudThrowExceptionOnUnknownStringPayload() {
        eduMessageStrategyFactory.create("dette burde ikke fungere");
    }

    @Test(expected = MeldingsUtvekslingRuntimeException.class)
    public void testShouldFailOnUnknownPayloadClass() {
        eduMessageStrategyFactory.create(1337);
    }
}