package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;

import static org.apache.commons.lang.StringEscapeUtils.unescapeHtml;
import static org.junit.Assert.assertEquals;

/**
 * TOOD: Add test for payload of type ElementNSImpl and other variants
 *
 * @author Glenn Bech
 */
public class JournalPostIdTest {

    String payloadAsString = "&lt;?xml version=\"1.0\" encoding=\"utf-8\"?&gt;\n" +
            "&lt;Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"&gt;\n" +
            "  &lt;journpost xmlns=\"\"&gt;\n" +
            "    &lt;jpId&gt;210570&lt;/jpId&gt;\n" +
            "    &lt;jpJaar&gt;2015&lt;/jpJaar&gt;\n" +
            "    &lt;jpSeknr&gt;41&lt;/jpSeknr&gt;\n" +
            "    &lt;jpJpostnr&gt;3&lt;/jpJpostnr&gt;\n" +
            "    &lt;jpJdato&gt;0001-01-01&lt;/jpJdato&gt;\n" +
            "    &lt;jpNdoktype&gt;U&lt;/jpNdoktype&gt;\n" +
            "    &lt;jpDokdato&gt;2015-09-07&lt;/jpDokdato&gt;\n" +
            "    &lt;jpStatus&gt;R&lt;/jpStatus&gt;\n" +
            "    &lt;jpInnhold&gt;Edu testdokument&lt;/jpInnhold&gt;\n" +
            "    &lt;jpForfdato /&gt;\n" +
            "    &lt;jpTgkode&gt;U&lt;/jpTgkode&gt;\n" +
            "    &lt;jpAgdato /&gt;\n" +
            "    &lt;jpAntved /&gt;\n" +
            "    &lt;jpSaar&gt;2015&lt;/jpSaar&gt;\n" +
            "    &lt;jpSaseknr&gt;20&lt;/jpSaseknr&gt;\n" +
            "    &lt;jpOffinnhold&gt;Edu testdokument&lt;/jpOffinnhold&gt;\n" +
            "    &lt;jpTggruppnavn&gt;Alle&lt;/jpTggruppnavn&gt;\n" +
            "    &lt;avsmot&gt;\n" +
            "     &lt;amIhtype&gt;0&lt;/amIhtype&gt;\n" +
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
            "      &lt;dbTittel&gt;Edu testdokument&lt;/dbTittel&gt;\n" +
            "      &lt;dbStatus&gt;B&lt;/dbStatus&gt;\n" +
            "      &lt;veVariant&gt;P&lt;/veVariant&gt;\n" +
            "      &lt;veDokformat&gt;DOCX&lt;/veDokformat&gt;\n" +
            "      &lt;fil&gt;\n" +
            "        &lt;base64&gt;UEsDBBQABgAIANdTJ0cOVawXUAIAAHoQAAATAAgCW0NCAgPC93OnJQcj4NCiAgICAgICAgICAgz4NCiAgICAgICAgICAgIDx3OnBsYWNlaG9sZGVyPg0KICAgICAgICAgICAgICA8dzpkb2NQYXJ0IHc6dmFsPlcjEueG1sUEsBAi0AFAAAAACaHAgB3b3JkL19yZWxzL3NldHRpbmdzLnhtbC5yZWxzUEsFBgAAAAArACsAbwsAAFuIAgAAAA==&lt;/base64&gt;\n" +
            "      &lt;/fil&gt;\n" +
            "      &lt;veFilnavn&gt;Edu testdokument.DOCX&lt;/veFilnavn&gt;\n" +
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
            "&lt;/Melding&gt;";

    PutMessageRequestType testDataWithStringPayload, tedtDataWithElementPayLoad;

    @Before
    public void createTestdata() throws JAXBException {
        testDataWithStringPayload = new PutMessageRequestType();
        EnvelopeType type = new EnvelopeType();
        testDataWithStringPayload.setEnvelope(type);
        testDataWithStringPayload.setPayload(unescapeHtml(payloadAsString));
    }

    @Test
    public void shouldExtractJpIdFromStringPayload() {
        JournalpostId id = JournalpostId.fromPutMessage(testDataWithStringPayload);
        assertEquals("210570", id.value());

    }

}
