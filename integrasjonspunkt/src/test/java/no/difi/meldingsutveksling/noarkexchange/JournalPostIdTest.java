package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * TOOD: Add test for payload of type ElementNSImpl and other variants
 *
 * @author Glenn Bech
 */
public class JournalPostIdTest {

    String cdataTaggedXml = "<PutMessageRequest xmlns=\"http://www.arkivverket.no/Noark/Exchange/types\">\n" +
            "    <envelope conversationId=\"19c73be0-f4fa-4c86-bc84-a2dfd912f948\"\n" +
            "              contentNamespace=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\" xmlns=\"\">\n" +
            "        <sender>\n" +
            "            <orgnr>974763907</orgnr>\n" +
            "            <name>Fylkesmannen i Sogn og Fjordane</name>\n" +
            "            <email>fmsfpost@fylkesmannen.no</email>\n" +
            "            <ref>2014/2703</ref>\n" +
            "        </sender>\n" +
            "        <receiver>\n" +
            "            <orgnr>974763907</orgnr>\n" +
            "            <name>Fylkesmannen i Sogn og Fjordane</name>\n" +
            "            <email>fmsfpost@fylkesmannen.no</email>\n" +
            "            <ref/>\n" +
            "        </receiver>\n" +
            "    </envelope>\n" +
            "    <payload xmlns=\"\"><![CDATA[<?xml version=\"1.0\" encoding=\"utf-8\"?><Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"><journpost xmlns=\"\"><jpId>219816</jpId><jpJaar>2015</jpJaar><jpSeknr>11734</jpSeknr><jpJpostnr>2</jpJpostnr><jpJdato>2015-10-08</jpJdato><jpNdoktype>U</jpNdoktype><jpDokdato>2015-10-08</jpDokdato><jpStatus>F</jpStatus><jpInnhold>Test1</jpInnhold><jpU1>0</jpU1><jpForfdato /><jpTgkode /><jpUoff /><jpAgdato /><jpAgkode /><jpSaksdel /><jpU2>0</jpU2><jpArkdel /><jpTlkode /><jpAntved>0</jpAntved><jpSaar>2014</jpSaar><jpSaseknr>2703</jpSaseknr><jpOffinnhold>Test1</jpOffinnhold><jpTggruppnavn /><avsmot><amId>501153</amId><amOrgnr>974763907</amOrgnr><amIhtype>1</amIhtype><amKopimot>0</amKopimot><amBehansv>0</amBehansv><amNavn>Fylkesmannen i Sogn og Fjordane</amNavn><amU1>0</amU1><amKortnavn>FMSF</amKortnavn><amAdresse>Njøsavegen 2</amAdresse><amPostnr>6863</amPostnr><amPoststed>Leikanger</amPoststed><amUtland /><amEpostadr>fmsfpost@fylkesmannen.no</amEpostadr><amRef /><amJenhet /><amAvskm /><amAvskdato /><amFrist /><amForsend>D</amForsend><amAdmkort>[Ufordelt]</amAdmkort><amAdmbet>Ufordelt/sendt tilbake til arkiv</amAdmbet><amSbhinit>[Ufordelt]</amSbhinit><amSbhnavn>Ikke fordelt til saksbehandler</amSbhnavn><amAvsavdok /><amBesvardok /></avsmot><dokument><dlRnr>1</dlRnr><dlType>H</dlType><dbKategori>ND</dbKategori><dbTittel>Test1</dbTittel><dbStatus>F</dbStatus><veVariant>A</veVariant><veDokformat>RA-PDF</veDokformat><fil><base64>aGVsbG8gd29ybGQ=</base64></fil><veFilnavn /><veMimeType /></dokument></journpost><noarksak xmlns=\"\"><saId>68286</saId><saSaar>2014</saSaar><saSeknr>2703</saSeknr><saPapir>0</saPapir><saDato>2014-11-27</saDato><saTittel>Test Knutepunkt herokuapp</saTittel><saU1>0</saU1><saStatus>B</saStatus><saArkdel>EARKIV1</saArkdel><saType /><saJenhet>SENTRAL</saJenhet><saTgkode /><saUoff /><saBevtid /><saKasskode /><saKassdato /><saProsjekt /><saOfftittel>Test Knutepunkt herokuapp</saOfftittel><saAdmkort>FM-ADMA</saAdmkort><saAdmbet>Administrasjon</saAdmbet><saAnsvinit>JPS</saAnsvinit><saAnsvnavn>John Petter Svedal</saAnsvnavn><saTggruppnavn /></noarksak></Melding>]]></payload></PutMessageRequest>";

    private JAXBContext jaxbContext;

    @BeforeEach
    public void initializeJaxb() throws JAXBException {
        jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
    }

    @Test
    public void shouldExtractJpIdFromCdataTaggedXml() throws JAXBException {
        PutMessageRequestType request = createPutMessageRequestWith(cdataTaggedXml);
        JournalpostId id = JournalpostId.fromPutMessage(new PutMessageRequestWrapper(request));
        assertEquals("219816", id.value());

    }

    private PutMessageRequestType createPutMessageRequestWith(String payload) throws JAXBException {


        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(new StringSource((payload)), PutMessageRequestType.class).getValue();
    }
}
