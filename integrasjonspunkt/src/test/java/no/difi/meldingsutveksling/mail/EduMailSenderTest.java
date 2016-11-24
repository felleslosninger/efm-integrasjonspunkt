package no.difi.meldingsutveksling.mail;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.receive.PutMessageRequestConverter;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.junit.Before;
import org.junit.Test;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class EduMailSenderTest {

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

    private ServiceRegistryLookup serviceRegistryLookup;
    private JAXBContext putMessageJaxbContext;
    private EduMailSender mailSender;
    private IntegrasjonspunktProperties props;

    @Before
    public void init() throws JAXBException {
        props = new IntegrasjonspunktProperties();
        mailSender = new EduMailSender(props);

        putMessageJaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);

        InfoRecord infoRecord = new InfoRecord("1234", "Foo", new EntityType("EDU", "EDU"));
        when(serviceRegistryLookup.getInfoRecord(anyString())).thenReturn(infoRecord);
    }

    @Test
    public void testSend() throws JAXBException {
        PutMessageRequestType putMessage = createPutMessageCdataXml(cdataTaggedXml);

        PutMessageRequestConverter converter = new PutMessageRequestConverter();
        byte[] bytes = converter.marshallToBytes(putMessage);
        mailSender.send(bytes, "foo");
    }

    private PutMessageRequestType createPutMessageCdataXml(String payload) throws JAXBException {
        Unmarshaller unmarshaller = putMessageJaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(new StringSource((payload)), PutMessageRequestType.class).getValue();
    }
}
