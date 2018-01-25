package no.difi.meldingsutveksling.core;

import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import no.difi.meldingsutveksling.nextmove.DpoConversationResource;
import no.difi.meldingsutveksling.nextmove.Receiver;
import no.difi.meldingsutveksling.nextmove.Sender;
import no.difi.meldingsutveksling.noarkexchange.PayloadException;
import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class EDUCoreFactoryTest {

    private static String escapedXml = "&lt;?xml version=\"1.0\" encoding=\"utf-8\"?&gt;\n" +
            "&lt;MeldingType xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"&gt;\n" +
            "  &lt;journpost xmlns=\"\"&gt;\n" +
            "    &lt;jpId&gt;210570&lt;/jpId&gt;\n" +
            "    &lt;jpJaar&gt;2015&lt;/jpJaar&gt;\n" +
            "    &lt;jpSeknr&gt;41&lt;/jpSeknr&gt;\n" +
            "    &lt;jpJpostnr&gt;3&lt;/jpJpostnr&gt;\n" +
            "    &lt;jpJdato&gt;0001-01-01&lt;/jpJdato&gt;\n" +
            "    &lt;jpNdoktype&gt;U&lt;/jpNdoktype&gt;\n" +
            "    &lt;jpDokdato&gt;2015-09-07&lt;/jpDokdato&gt;\n" +
            "    &lt;jpStatus&gt;R&lt;/jpStatus&gt;\n" +
            "    &lt;jpInnhold&gt;Test1&lt;/jpInnhold&gt;\n" +
            "    &lt;jpForfdato /&gt;\n" +
            "    &lt;jpTgkode&gt;U&lt;/jpTgkode&gt;\n" +
            "    &lt;jpAgdato /&gt;\n" +
            "    &lt;jpAntved /&gt;\n" +
            "    &lt;jpSaar&gt;2015&lt;/jpSaar&gt;\n" +
            "    &lt;jpSaseknr&gt;20&lt;/jpSaseknr&gt;\n" +
            "    &lt;jpOffinnhold&gt;Test2&lt;/jpOffinnhold&gt;\n" +
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
            "&lt;/MeldingType&gt;";
    
    private static String xmlString = "<Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\">\n" +
            "<journpost xmlns=\"\"><jpId>219816</jpId><jpJaar>2015</jpJaar><jpSeknr>11734</jpSeknr><jpJpostnr>2</jpJpostnr><jpJdato>2015-10-08\n" +
            "</jpJdato><jpNdoktype>U</jpNdoktype><jpDokdato>2015-10-08</jpDokdato><jpStatus>F</jpStatus><jpInnhold>Test1\n" +
            "</jpInnhold><jpU1>0</jpU1><jpForfdato/><jpTgkode/><jpUoff/><jpAgdato/><jpAgkode/><jpSaksdel/><jpU2>0\n" +
            "</jpU2><jpArkdel/><jpTlkode/><jpAntved>0</jpAntved><jpSaar>2014</jpSaar><jpSaseknr>2703</jpSaseknr><jpOffinnhold>Test2\n" +
            "</jpOffinnhold><jpTggruppnavn/><avsmot>\n" +
            "<amId>501153</amId>\n" +
            "<amOrgnr>974763907</amOrgnr>\n" +
            "<amIhtype>1</amIhtype>\n" +
            "<amKopimot>0</amKopimot>\n" +
            "<amBehansv>0</amBehansv>\n" +
            "<amNavn>Fylkesmannen i Sogn og Fjordane</amNavn>\n" +
            "<amU1>0</amU1>\n" +
            "<amKortnavn>FMSF</amKortnavn>\n" +
            "<amAdresse>Njøsavegen 2</amAdresse>\n" +
            "<amPostnr>6863</amPostnr>\n" +
            "<amPoststed>Leikanger</amPoststed>\n" +
            "<amUtland/>\n" +
            "<amEpostadr>fmsfpost@fylkesmannen.no</amEpostadr>\n" +
            "<amRef/>\n" +
            "<amJenhet/>\n" +
            "<amAvskm/>\n" +
            "<amAvskdato/>\n" +
            "<amFrist/>\n" +
            "<amForsend>D</amForsend>\n" +
            "<amAdmkort>[Ufordelt]</amAdmkort>\n" +
            "<amAdmbet>Ufordelt/sendt tilbake til arkiv</amAdmbet>\n" +
            "<amSbhinit>[Ufordelt]</amSbhinit>\n" +
            "<amSbhnavn>Ikke fordelt til saksbehandler</amSbhnavn>\n" +
            "<amAvsavdok/>\n" +
            "<amBesvardok/>\n" +
            "</avsmot><dokument>\n" +
            "<dlRnr>1</dlRnr>\n" +
            "<dlType>H</dlType>\n" +
            "<dbKategori>ND</dbKategori>\n" +
            "<dbTittel>Test1</dbTittel>\n" +
            "<dbStatus>F</dbStatus>\n" +
            "<veVariant>A</veVariant>\n" +
            "<veDokformat>RA-PDF</veDokformat>\n" +
            "<fil>\n" +
            "    <base64>aGVsbG8gd29ybGQ=</base64>\n" +
            "</fil>\n" +
            "<veFilnavn>Edu testdokument.DOCX</veFilnavn>\n" +
            "<veMimeType/>\n" +
            "</dokument></journpost>\n" +
            "<noarksak xmlns=\"\"><saId>68286</saId><saSaar>2014</saSaar><saSeknr>2703</saSeknr><saPapir>0\n" +
            "</saPapir><saDato>2014-11-27</saDato><saTittel>Test Knutepunkt herokuapp</saTittel><saU1>0</saU1><saStatus>B\n" +
            "</saStatus><saArkdel>EARKIV1</saArkdel><saType/><saJenhet>SENTRAL\n" +
            "</saJenhet><saTgkode/><saUoff/><saBevtid/><saKasskode/><saKassdato/><saProsjekt/><saOfftittel>Test Knutepunkt\n" +
            "herokuapp\n" +
            "</saOfftittel><saAdmkort>FM-ADMA</saAdmkort><saAdmbet>Administrasjon</saAdmbet><saAnsvinit>JPS</saAnsvinit><saAnsvnavn>\n" +
            "John Petter Svedal\n" +
            "</saAnsvnavn><saTggruppnavn/></noarksak></Melding>";

    private static String cdataTaggedXml = "<PutMessageRequest xmlns=\"http://www.arkivverket.no/Noark/Exchange/types\">\n" +
            "    <envelope conversationId=\"19c73be0-f4fa-4c86-bc84-a2dfd912f948\"\n" +
            "              contentNamespace=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\" xmlns=\"\">\n" +
            "        <sender>\n" +
            "            <orgnr>910075918</orgnr>\n" +
            "            <name>Fylkesmannen i Sogn og Fjordane</name>\n" +
            "            <email>fmsfpost@fylkesmannen.no</email>\n" +
            "            <ref>2014/2703</ref>\n" +
            "        </sender>\n" +
            "        <receiver>\n" +
            "            <orgnr>910075918</orgnr>\n" +
            "            <name>Fylkesmannen i Sogn og Fjordane</name>\n" +
            "            <email>fmsfpost@fylkesmannen.no</email>\n" +
            "            <ref/>\n" +
            "        </receiver>\n" +
            "    </envelope>\n" +
            "    <payload xmlns=\"\"><![CDATA[<?xml version=\"1.0\" encoding=\"utf-8\"?><Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"><journpost xmlns=\"\"><jpId>219816</jpId><jpJaar>2015</jpJaar><jpSeknr>11734</jpSeknr><jpJpostnr>2</jpJpostnr><jpJdato>2015-10-08</jpJdato><jpNdoktype>U</jpNdoktype><jpDokdato>2015-10-08</jpDokdato><jpStatus>F</jpStatus><jpInnhold>Test1</jpInnhold><jpU1>0</jpU1><jpForfdato /><jpTgkode /><jpUoff /><jpAgdato /><jpAgkode /><jpSaksdel /><jpU2>0</jpU2><jpArkdel /><jpTlkode /><jpAntved>0</jpAntved><jpSaar>2014</jpSaar><jpSaseknr>2703</jpSaseknr><jpOffinnhold>Test2</jpOffinnhold><jpTggruppnavn /><avsmot><amId>501153</amId><amOrgnr>974763907</amOrgnr><amIhtype>1</amIhtype><amKopimot>0</amKopimot><amBehansv>0</amBehansv><amNavn>Fylkesmannen i Sogn og Fjordane</amNavn><amU1>0</amU1><amKortnavn>FMSF</amKortnavn><amAdresse>Njøsavegen 2</amAdresse><amPostnr>6863</amPostnr><amPoststed>Leikanger</amPoststed><amUtland /><amEpostadr>fmsfpost@fylkesmannen.no</amEpostadr><amRef /><amJenhet /><amAvskm /><amAvskdato /><amFrist /><amForsend>D</amForsend><amAdmkort>[Ufordelt]</amAdmkort><amAdmbet>Ufordelt/sendt tilbake til arkiv</amAdmbet><amSbhinit>[Ufordelt]</amSbhinit><amSbhnavn>Ikke fordelt til saksbehandler</amSbhnavn><amAvsavdok /><amBesvardok /></avsmot><dokument><dlRnr>1</dlRnr><dlType>H</dlType><dbKategori>ND</dbKategori><dbTittel>Test1</dbTittel><dbStatus>F</dbStatus><veVariant>A</veVariant><veDokformat>RA-PDF</veDokformat><fil><base64>aGVsbG8gd29ybGQ=</base64></fil><veFilnavn>Edu testdokument.DOCX</veFilnavn><veMimeType /></dokument></journpost><noarksak xmlns=\"\"><saId>68286</saId><saSaar>2014</saSaar><saSeknr>2703</saSeknr><saPapir>0</saPapir><saDato>2014-11-27</saDato><saTittel>Test Knutepunkt herokuapp</saTittel><saU1>0</saU1><saStatus>B</saStatus><saArkdel>EARKIV1</saArkdel><saType /><saJenhet>SENTRAL</saJenhet><saTgkode /><saUoff /><saBevtid /><saKasskode /><saKassdato /><saProsjekt /><saOfftittel>Test Knutepunkt herokuapp</saOfftittel><saAdmkort>FM-ADMA</saAdmkort><saAdmbet>Administrasjon</saAdmbet><saAnsvinit>JPS</saAnsvinit><saAnsvnavn>John Petter Svedal</saAnsvnavn><saTggruppnavn /></noarksak></Melding>]]></payload></PutMessageRequest>";

    private static String cdataTaggedMxaXml = "<Message batchSending=\"1\" domain=\"PT\" sendingSystem=\"AGENCY\"\n" +
            "xsi:noNamespaceSchemaLocation=\"sant_mxa.xsd\"\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "caseDescription=\"test message example\" caseOfficer=\"ttt,Mr. Test\">\n" +
            "<ParticipantId>910075918</ParticipantId>\n" +
            "<MessageReference>P1234-5-test</MessageReference>\n" +
            "<Idproc>66A50D806D9444E5E044000E7F7E0BD2</Idproc>\n" +
            "<DueDate>2009-04-07</DueDate>\n" +
            "<AltinnArchive>AM12345</AltinnArchive>\n" +
            "<Content>\n" +
            "    <MessageHeader><![CDATA[Test1]]></MessageHeader>\n" +
            "    <MessageSummery><![CDATA[Test2]]></MessageSummery>    \n" +
            "    <Attachments>\n" +
            "      <Attachment filename=\"Edu testdokument.DOCX\" name=\"Vedlegg 1\"\n" +
            "        mimeType=\"text/plain\">RGV0dGUgZXIgZmlsIG51bW1lciAxLgoK</Attachment>\n" +
            "      <Attachment filename=\"filename.txt\" name=\"Vedlegg 1\"\n" +
            "        mimeType=\"text/plain\">SGVyIGVyIGZpbCBudW1tZXIgMi4KCg==</Attachment>\n" +
            "      <Attachment filename=\"filename.txt\" name=\"Vedlegg 1\"\n" +
            "        mimeType=\"text/plain\">VHJlZGplIGZpbGVuIGVyIGRlbm5lLgo=</Attachment>\n" +
            "    </Attachments>\n" +
            "  </Content>\n" +
            "  <NotificationMessages>\n" +
            "    <NotificationMessage>\n" +
            "      <SMSPhoneNumbers>\n" +
            "        <SMSPhoneNumber>+4700000000</SMSPhoneNumber>\n" +
            "      </SMSPhoneNumbers>\n" +
            "      <EmailAddresses>\n" +
            "                <EmailAddress>oslo@foo.bar</EmailAddress>\n" +
            "            </EmailAddresses>\n" +
            "    </NotificationMessage>\n" +
            "  </NotificationMessages>\n" +
            "</Message>";

    private JAXBContext putMessageJaxbContext;
    private ServiceRegistryLookup serviceRegistryLookup;
    private JAXBContext mxaMessageJaxbContext;

    @Before
    public void init() throws JAXBException {
        putMessageJaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
        mxaMessageJaxbContext = JAXBContext.newInstance(Message.class);
        serviceRegistryLookup = Mockito.mock(ServiceRegistryLookup.class);

        InfoRecord infoRecord = new InfoRecord("1234", "Foo", new EntityType("Organisasjonsledd", "ORGL"));
        ServiceRecord serviceRecord = new ServiceRecord(ServiceIdentifier.DPO, "1234", "pem123", "http://foo");
        when(serviceRegistryLookup.getInfoRecord(anyString())).thenReturn(infoRecord);
        when(serviceRegistryLookup.getServiceRecord(anyString())).thenReturn(serviceRecord);
    }

    @Test
    public void testUnmarshallPayload() throws JAXBException {
        EDUCoreFactory eduCoreFactory = new EDUCoreFactory(serviceRegistryLookup);
        MeldingType meldingType = EDUCoreConverter.payloadAsMeldingType(StringEscapeUtils.unescapeXml(escapedXml));
        assertEquals("210570", meldingType.getJournpost().getJpId());
    }

    @Test
    public void testCreateFromPutMessage() throws JAXBException, PayloadException {
        EDUCoreFactory eduCoreFactory = new EDUCoreFactory(serviceRegistryLookup);
        PutMessageRequestType putMessage = createPutMessageCdataXml(cdataTaggedXml);

        EDUCore eduCore = eduCoreFactory.create(putMessage, "1234");
        assertEquals("219816", PayloadUtil.queryJpId(eduCore.getPayload()));

    }

    @Test
    public void testCreateFromMXAMessage() throws JAXBException, PayloadException {
        Message message = createMxaMessageEscapedXml(cdataTaggedMxaXml);
        EDUCoreFactory eduCoreFactory = new EDUCoreFactory(serviceRegistryLookup);

        EDUCore eduCore = eduCoreFactory.create(message, "1234");
        assertEquals("P1234-5-test", PayloadUtil.queryJpId(eduCore.getPayload()));
    }

    @Test
    public void testPutMessageFromEduCore() throws JAXBException {
        EDUCoreFactory eduCoreFactory = new EDUCoreFactory(serviceRegistryLookup);
        PutMessageRequestType putMessage = createPutMessageCdataXml(cdataTaggedXml);

        EDUCore eduCore = eduCoreFactory.create(putMessage, "1234");
        PutMessageRequestType message = eduCoreFactory.createPutMessageFromCore(eduCore);
        assertEquals("19c73be0-f4fa-4c86-bc84-a2dfd912f948", message.getEnvelope().getConversationId());
    }

    @Test
    public void testCreateEducoreFromArkivmelding() throws IOException, JAXBException {
        String convId = "3380ed76-5d4c-43e7-aa70-8ed8d97e4835";
        File arkivmeldingFile = new File("src/test/resources/arkivmelding_ok.xml");
        File testZipFile = new File("src/test/resources/test.zip");
        byte[] arkivmeldingBytes = FileUtils.readFileToByteArray(arkivmeldingFile);
        byte[] zipBytes = FileUtils.readFileToByteArray(testZipFile);

        JAXBContext jaxbContext = JAXBContext.newInstance(Arkivmelding.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Arkivmelding am = unmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(arkivmeldingBytes)), Arkivmelding.class).getValue();

        DpoConversationResource cr = DpoConversationResource.of(convId, Sender.of("123", "foo"), Receiver.of("321", "bar"));

        EDUCoreFactory eduCoreFactory = new EDUCoreFactory(serviceRegistryLookup);
        EDUCore eduCore = eduCoreFactory.create(cr, am, zipBytes);
        MeldingType meldingType = EDUCoreConverter.payloadAsMeldingType(eduCore.getPayload());

        assertEquals(convId, eduCore.getMessageReference());
        assertEquals("Blah", meldingType.getNoarksak().getSaAdmkort());
        assertEquals("test.pdf", meldingType.getJournpost().getDokument().get(0).getVeFilnavn());
    }

    private PutMessageRequestType createPutMessageCdataXml(String payload) throws JAXBException {
        Unmarshaller unmarshaller = putMessageJaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(new StringSource((payload)), PutMessageRequestType.class).getValue();
    }

    private Message createMxaMessageEscapedXml(String payload) throws JAXBException {
        Unmarshaller unmarshaller = mxaMessageJaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(new StringSource((payload)), Message.class).getValue();
    }
}
