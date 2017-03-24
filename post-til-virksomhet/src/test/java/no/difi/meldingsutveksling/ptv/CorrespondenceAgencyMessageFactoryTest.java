package no.difi.meldingsutveksling.ptv;

import no.altinn.services.serviceengine.correspondence._2009._10.InsertCorrespondenceV2;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import no.difi.meldingsutveksling.noarkexchange.PayloadException;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.EnvelopeType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.Before;
import org.junit.Test;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testclass for {@link CorrespondenceAgencyMessageFactory}
 * <p>
 * Created by kons-mwa on 01.06.2016.
 */
public class CorrespondenceAgencyMessageFactoryTest {

    private static String escapedXml = "&lt;?xml version=\"1.0\" encoding=\"utf-8\"?&gt;\n"
            + "&lt;Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"&gt;\n"
            + "  &lt;journpost xmlns=\"\"&gt;\n"
            + "    &lt;jpId&gt;210570&lt;/jpId&gt;\n"
            + "    &lt;jpJaar&gt;2015&lt;/jpJaar&gt;\n"
            + "    &lt;jpSeknr&gt;41&lt;/jpSeknr&gt;\n"
            + "    &lt;jpJpostnr&gt;3&lt;/jpJpostnr&gt;\n"
            + "    &lt;jpJdato&gt;0001-01-01&lt;/jpJdato&gt;\n"
            + "    &lt;jpNdoktype&gt;U&lt;/jpNdoktype&gt;\n"
            + "    &lt;jpDokdato&gt;2015-09-07&lt;/jpDokdato&gt;\n"
            + "    &lt;jpStatus&gt;R&lt;/jpStatus&gt;\n"
            + "    &lt;jpInnhold&gt;Test1&lt;/jpInnhold&gt;\n"
            + "    &lt;jpForfdato /&gt;\n"
            + "    &lt;jpTgkode&gt;U&lt;/jpTgkode&gt;\n"
            + "    &lt;jpAgdato /&gt;\n"
            + "    &lt;jpAntved /&gt;\n"
            + "    &lt;jpSaar&gt;2015&lt;/jpSaar&gt;\n"
            + "    &lt;jpSaseknr&gt;20&lt;/jpSaseknr&gt;\n"
            + "    &lt;jpOffinnhold&gt;Test2&lt;/jpOffinnhold&gt;\n"
            + "    &lt;jpTggruppnavn&gt;Alle&lt;/jpTggruppnavn&gt;\n"
            + "    &lt;avsmot&gt;\n"
            + "     &lt;amIhtype&gt;0&lt;/amIhtype&gt;\n"
            + "      &lt;amNavn&gt;Saksbehandler Testbruker7&lt;/amNavn&gt;\n"
            + "      &lt;amAdresse&gt;Postboks 8115 Dep.&lt;/amAdresse&gt;\n"
            + "      &lt;amPostnr&gt;0032&lt;/amPostnr&gt;\n"
            + "      &lt;amPoststed&gt;OSLO&lt;/amPoststed&gt;\n"
            + "      &lt;amUtland&gt;Norge&lt;/amUtland&gt;\n"
            + "      &lt;amEpostadr&gt;sa-user.test2@difi.no&lt;/amEpostadr&gt;\n"
            + "    &lt;/avsmot&gt;\n"
            + "    &lt;avsmot&gt;\n"
            + "      &lt;amOrgnr&gt;974720760&lt;/amOrgnr&gt;\n"
            + "      &lt;amIhtype&gt;1&lt;/amIhtype&gt;\n"
            + "      &lt;amNavn&gt;EduTestOrg 1&lt;/amNavn&gt;\n"
            + "    &lt;/avsmot&gt;\n"
            + "    &lt;dokument&gt;\n"
            + "      &lt;dlRnr&gt;1&lt;/dlRnr&gt;\n"
            + "      &lt;dlType&gt;H&lt;/dlType&gt;\n"
            + "      &lt;dbTittel&gt;Edu testdokument&lt;/dbTittel&gt;\n"
            + "      &lt;dbStatus&gt;B&lt;/dbStatus&gt;\n"
            + "      &lt;veVariant&gt;P&lt;/veVariant&gt;\n"
            + "      &lt;veDokformat&gt;DOCX&lt;/veDokformat&gt;\n"
            + "      &lt;fil&gt;\n"
            + "        &lt;base64&gt;UEsDBBQABgAIANdTJ0cOVawXUAIAAHoQAAATAAgCW0NCAgPC93OnJQcj4NCiAgICAgICAgICAgz4NCiAgICAgICAgICAgIDx3OnBsYWNlaG9sZGVyPg0KICAgICAgICAgICAgICA8dzpkb2NQYXJ0IHc6dmFsPlcjEueG1sUEsBAi0AFAAAAACaHAgB3b3JkL19yZWxzL3NldHRpbmdzLnhtbC5yZWxzUEsFBgAAAAArACsAbwsAAFuIAgAAAA==&lt;/base64&gt;\n"
            + "      &lt;/fil&gt;\n"
            + "      &lt;veFilnavn&gt;Edu testdokument.DOCX&lt;/veFilnavn&gt;\n"
            + "      &lt;veMimeType&gt;application/vnd.openxmlformats-officedocument.wordprocessingml.document&lt;/veMimeType&gt;\n"
            + "    &lt;/dokument&gt;\n"
            + "  &lt;/journpost&gt;\n"
            + "  &lt;noarksak xmlns=\"\"&gt;\n"
            + "    &lt;saId&gt;15/00020&lt;/saId&gt;\n"
            + "    &lt;saSaar&gt;2015&lt;/saSaar&gt;\n"
            + "    &lt;saSeknr&gt;20&lt;/saSeknr&gt;\n"
            + "    &lt;saPapir&gt;0&lt;/saPapir&gt;\n"
            + "    &lt;saDato&gt;2015-09-01&lt;/saDato&gt;\n"
            + "    &lt;saTittel&gt;BEST/EDU testsak&lt;/saTittel&gt;\n"
            + "    &lt;saStatus&gt;R&lt;/saStatus&gt;\n"
            + "    &lt;saArkdel&gt;Sakarkiv 2013&lt;/saArkdel&gt;\n"
            + "    &lt;saType&gt;Sak&lt;/saType&gt;\n"
            + "    &lt;saJenhet&gt;Oslo&lt;/saJenhet&gt;\n"
            + "    &lt;saTgkode&gt;U&lt;/saTgkode&gt;\n"
            + "    &lt;saBevtid /&gt;\n"
            + "    &lt;saKasskode&gt;B&lt;/saKasskode&gt;\n"
            + "    &lt;saOfftittel&gt;BEST/EDU testsak&lt;/saOfftittel&gt;\n"
            + "    &lt;saAdmkort&gt;202286&lt;/saAdmkort&gt;\n"
            + "    &lt;saAdmbet&gt;Seksjon for test 1&lt;/saAdmbet&gt;\n"
            + "    &lt;saAnsvinit&gt;difi\\sa-user-test2&lt;/saAnsvinit&gt;\n"
            + "    &lt;saAnsvnavn&gt;Saksbehandler Testbruker7&lt;/saAnsvnavn&gt;\n"
            + "    &lt;saTggruppnavn&gt;Alle&lt;/saTggruppnavn&gt;\n"
            + "    &lt;sakspart&gt;\n"
            + "      &lt;spId&gt;0&lt;/spId&gt;\n"
            + "    &lt;/sakspart&gt;\n"
            + "  &lt;/noarksak&gt;\n"
            + "&lt;/Melding&gt;";

    private static String cdataTaggedXml = "<PutMessageRequest xmlns=\"http://www.arkivverket.no/Noark/Exchange/types\">\n"
            + "    <envelope conversationId=\"19c73be0-f4fa-4c86-bc84-a2dfd912f948\"\n"
            + "              contentNamespace=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\" xmlns=\"\">\n"
            + "        <sender>\n"
            + "            <orgnr>910075918</orgnr>\n"
            + "            <name>Fylkesmannen i Sogn og Fjordane</name>\n"
            + "            <email>fmsfpost@fylkesmannen.no</email>\n"
            + "            <ref>2014/2703</ref>\n"
            + "        </sender>\n"
            + "        <receiver>\n"
            + "            <orgnr>910075918</orgnr>\n"
            + "            <name>Fylkesmannen i Sogn og Fjordane</name>\n"
            + "            <email>fmsfpost@fylkesmannen.no</email>\n"
            + "            <ref/>\n"
            + "        </receiver>\n"
            + "    </envelope>\n"
            + "    <payload xmlns=\"\"><![CDATA[<?xml version=\"1.0\" encoding=\"utf-8\"?><Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"><journpost xmlns=\"\"><jpId>219816</jpId><jpJaar>2015</jpJaar><jpSeknr>11734</jpSeknr><jpJpostnr>2</jpJpostnr><jpJdato>2015-10-08</jpJdato><jpNdoktype>U</jpNdoktype><jpDokdato>2015-10-08</jpDokdato><jpStatus>F</jpStatus><jpInnhold>Test1</jpInnhold><jpU1>0</jpU1><jpForfdato /><jpTgkode /><jpUoff /><jpAgdato /><jpAgkode /><jpSaksdel /><jpU2>0</jpU2><jpArkdel /><jpTlkode /><jpAntved>0</jpAntved><jpSaar>2014</jpSaar><jpSaseknr>2703</jpSaseknr><jpOffinnhold>Test2</jpOffinnhold><jpTggruppnavn /><avsmot><amId>501153</amId><amOrgnr>974763907</amOrgnr><amIhtype>1</amIhtype><amKopimot>0</amKopimot><amBehansv>0</amBehansv><amNavn>Fylkesmannen i Sogn og Fjordane</amNavn><amU1>0</amU1><amKortnavn>FMSF</amKortnavn><amAdresse>Njøsavegen 2</amAdresse><amPostnr>6863</amPostnr><amPoststed>Leikanger</amPoststed><amUtland /><amEpostadr>fmsfpost@fylkesmannen.no</amEpostadr><amRef /><amJenhet /><amAvskm /><amAvskdato /><amFrist /><amForsend>D</amForsend><amAdmkort>[Ufordelt]</amAdmkort><amAdmbet>Ufordelt/sendt tilbake til arkiv</amAdmbet><amSbhinit>[Ufordelt]</amSbhinit><amSbhnavn>Ikke fordelt til saksbehandler</amSbhnavn><amAvsavdok /><amBesvardok /></avsmot><dokument><dlRnr>1</dlRnr><dlType>H</dlType><dbKategori>ND</dbKategori><dbTittel>Test1</dbTittel><dbStatus>F</dbStatus><veVariant>A</veVariant><veDokformat>RA-PDF</veDokformat><fil><base64>aGVsbG8gd29ybGQ=</base64></fil><veFilnavn>Edu testdokument.DOCX</veFilnavn><veMimeType /></dokument></journpost><noarksak xmlns=\"\"><saId>68286</saId><saSaar>2014</saSaar><saSeknr>2703</saSeknr><saPapir>0</saPapir><saDato>2014-11-27</saDato><saTittel>Test Knutepunkt herokuapp</saTittel><saU1>0</saU1><saStatus>B</saStatus><saArkdel>EARKIV1</saArkdel><saType /><saJenhet>SENTRAL</saJenhet><saTgkode /><saUoff /><saBevtid /><saKasskode /><saKassdato /><saProsjekt /><saOfftittel>Test Knutepunkt herokuapp</saOfftittel><saAdmkort>FM-ADMA</saAdmkort><saAdmbet>Administrasjon</saAdmbet><saAnsvinit>JPS</saAnsvinit><saAnsvnavn>John Petter Svedal</saAnsvnavn><saTggruppnavn /></noarksak></Melding>]]></payload></PutMessageRequest>";

    private static String cdataTaggedMxaXml = "<Message batchSending=\"1\" domain=\"PT\" sendingSystem=\"AGENCY\"\n"
            + "xsi:noNamespaceSchemaLocation=\"sant_mxa.xsd\"\n"
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "caseDescription=\"test message example\" caseOfficer=\"ttt,Mr. Test\">\n"
            + "<ParticipantId>910075918</ParticipantId>\n"
            + "<MessageReference>P1234-5-test</MessageReference>\n"
            + "<Idproc>66A50D806D9444E5E044000E7F7E0BD2</Idproc>\n"
            + "<DueDate>2009-04-07</DueDate>\n"
            + "<AltinnArchive>AM12345</AltinnArchive>\n"
            + "<Content>\n"
            + "    <MessageHeader><![CDATA[Test1]]></MessageHeader>\n"
            + "    <MessageSummery><![CDATA[Test2]]></MessageSummery>    \n"
            + "    <Attachments>\n"
            + "      <Attachment filename=\"Edu testdokument.DOCX\" name=\"Vedlegg 1\"\n"
            + "        mimeType=\"text/plain\">RGV0dGUgZXIgZmlsIG51bW1lciAxLgoK</Attachment>\n"
            + "      <Attachment filename=\"filename.txt\" name=\"Vedlegg 1\"\n"
            + "        mimeType=\"text/plain\">SGVyIGVyIGZpbCBudW1tZXIgMi4KCg==</Attachment>\n"
            + "      <Attachment filename=\"filename.txt\" name=\"Vedlegg 1\"\n"
            + "        mimeType=\"text/plain\">VHJlZGplIGZpbGVuIGVyIGRlbm5lLgo=</Attachment>\n"
            + "    </Attachments>\n"
            + "  </Content>\n"
            + "  <NotificationMessages>\n"
            + "    <NotificationMessage>\n"
            + "      <SMSPhoneNumbers>\n"
            + "        <SMSPhoneNumber>+4700000000</SMSPhoneNumber>\n"
            + "      </SMSPhoneNumbers>\n"
            + "      <EmailAddresses>\n"
            + "                <EmailAddress>oslo@foo.bar</EmailAddress>\n"
            + "            </EmailAddresses>\n"
            + "    </NotificationMessage>\n"
            + "  </NotificationMessages>\n"
            + "</Message>";

    private JAXBContext putMessageJaxbContext;
    private JAXBContext mxaMessageJaxbContext;
    private CorrespondenceAgencyConfiguration postConfig;
    private ServiceRegistryLookup srMock;
    private EDUCoreFactory eduCoreFactory;

    @Before
    public void initializeJaxb() throws JAXBException {
        putMessageJaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
        mxaMessageJaxbContext = JAXBContext.newInstance(Message.class);

        postConfig = mock(CorrespondenceAgencyConfiguration.class);
        when(postConfig.getSystemUserCode()).thenReturn("AAS_TEST");

        srMock = mock(ServiceRegistryLookup.class);
        InfoRecord infoRecord = new InfoRecord("910075918", "Fylkesmannen i Sogn og Fjordane", new EntityType("DPV", "DPV"));
        ServiceRecord serviceRecord = new ServiceRecord(ServiceIdentifier.DPO, "1234", "pem123", "http://foo");
        when(srMock.getInfoRecord(any())).thenReturn(infoRecord);
        when(srMock.getServiceRecord(any())).thenReturn(serviceRecord);

        eduCoreFactory = new EDUCoreFactory(srMock);
    }

    @Test
    public void testFactoryForEscapedXMLPutMessage() throws PayloadException, JAXBException {
        EDUCore eduCore = eduCoreFactory.create(createPutMessageEscapedXml(escapedXml), "910075918");
        assertFields(CorrespondenceAgencyMessageFactory.create(postConfig, eduCore));
    }

    @Test
    public void testFactoryForEscapedXMLMXAMessage() throws PayloadException, JAXBException {
        EDUCore eduCore = eduCoreFactory.create(createMxaMessageEscapedXml(cdataTaggedMxaXml), "910075918");
        assertFields(CorrespondenceAgencyMessageFactory.create(postConfig, eduCore));
    }

    @Test
    public void testFactoryForCDataXML() throws PayloadException, JAXBException {
        EDUCore eduCore = eduCoreFactory.create(createPutMessageCdataXml(cdataTaggedXml), "910075918");
        assertFields(CorrespondenceAgencyMessageFactory.create(postConfig, eduCore));
    }

    private void assertFields(InsertCorrespondenceV2 c) {
        assertEquals("AAS_TEST", c.getSystemUserCode());
        assertEquals("4255", c.getCorrespondence().getServiceCode().getValue());
        assertEquals("10", c.getCorrespondence().getServiceEdition().getValue());
        assertEquals("910075918", c.getCorrespondence().getReportee().getValue());
        assertEquals("1044", c.getCorrespondence().getContent().getValue().getLanguageCode().getValue());
        assertEquals("Test1", c.getCorrespondence().getContent().getValue().getMessageTitle().getValue());
        assertEquals("Test1", c.getCorrespondence().getContent().getValue().getMessageSummary().getValue());
        assertEquals("Test2", c.getCorrespondence().getContent().getValue().getMessageBody().getValue());
        assertEquals("Edu testdokument.DOCX", c.getCorrespondence().getContent().getValue()
                .getAttachments().getValue().getBinaryAttachments().getValue()
                .getBinaryAttachmentV2().get(0).getFileName().getValue());
        assertEquals("Administrasjon", c.getCorrespondence().getNotifications().getValue()
                .getNotification().get(0).getTextTokens().getValue()
                .getTextToken().get(1).getTokenValue().getValue());
    }

    private PutMessageRequestType createPutMessageEscapedXml(String payload) {
        PutMessageRequestType request = new PutMessageRequestType();

        AddressType receiver = new AddressType();
        receiver.setOrgnr("910075918");

        AddressType sender = new AddressType();
        sender.setName("Fylkesmannen i Sogn og Fjordane");

        EnvelopeType envelopeType = new EnvelopeType();
        envelopeType.setReceiver(receiver);
        envelopeType.setSender(sender);

        request.setEnvelope(envelopeType);
        request.setPayload(payload);
        return request;
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
