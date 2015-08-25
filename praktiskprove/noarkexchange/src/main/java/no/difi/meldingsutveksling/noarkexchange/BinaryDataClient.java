package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.BindingProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BinaryDataClient {

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        long backThen = System.currentTimeMillis();
        if (args.length != 4) {
            System.out.println("Usage: BinaryDataClient {fromOrgNr} {toOrgNr} {filename} {endPontURL}");
            System.exit(-1);
        }

        String from = args[0];
        String to = args[1];
        String fileName = args[2];
        String endPointURL = args[3];

        ObjectFactory f = new ObjectFactory();
        PutMessageRequestType pmrt = new PutMessageRequestType();
        EnvelopeType envelope = new EnvelopeType();

        AddressType sender = new AddressType();
        sender.setOrgnr(from);
        envelope.setSender(sender);

        AddressType receiver = new AddressType();
        receiver.setOrgnr(to);
        envelope.setReceiver(sender);

        pmrt.setEnvelope(envelope);


        String bestEDUMessage = "<?xml version=\"1.0\" encoding=\"utf-8\"?><Melding xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.arkivverket.no/Noark4-1-WS-WD/types\"><journpost xmlns=\"\"><jpId>219759</jpId><jpJaar>2014</jpJaar><jpSeknr>11686</jpSeknr><jpJpostnr>1</jpJpostnr><jpJdato>2014-11-27</jpJdato><jpNdoktype>U</jpNdoktype><jpDokdato>2014-11-27</jpDokdato><jpStatus>F</jpStatus><jpInnhold>Test MSH Difi</jpInnhold><jpU1>0</jpU1><jpForfdato /><jpTgkode /><jpUoff /><jpAgdato /><jpAgkode /><jpSaksdel /><jpU2>0</jpU2><jpArkdel /><jpTlkode /><jpAntved>0</jpAntved><jpSaar>2014</jpSaar><jpSaseknr>2703</jpSaseknr><jpOffinnhold>Test MSH Difi</jpOffinnhold><jpTggruppnavn /><avsmot><amId>501041</amId><amOrgnr>974764687</amOrgnr><amIhtype>1</amIhtype><amKopimot>0</amKopimot><amBehansv>0</amBehansv><amNavn>Fylkesmannen i Nordland</amNavn><amU1>0</amU1><amKortnavn>FMNO</amKortnavn><amAdresse>Moloveien 10</amAdresse><amPostnr>8002</amPostnr><amPoststed>BODØ</amPoststed><amUtland /><amEpostadr>postmottak@fmno.no</amEpostadr><amRef /><amJenhet /><amAvskm /><amAvskdato /><amFrist /><amForsend>D</amForsend><amAdmkort>[Ufordelt]</amAdmkort><amAdmbet>Ufordelt/sendt tilbake til arkiv</amAdmbet><amSbhinit>[Ufordelt]</amSbhinit><amSbhnavn>Ikke fordelt til saksbehandler</amSbhnavn><amAvsavdok /><amBesvardok /></avsmot><dokument><dlRnr>1</dlRnr><dlType>H</dlType><dbKategori>ND</dbKategori><dbTittel>Test MSH Difi</dbTittel><dbStatus>F</dbStatus><veVariant>A</veVariant><veDokformat>RA-PDF</veDokformat><fil><base64>" + readFile(fileName) + "</base64></fil><veFilnavn /><veMimeType /></dokument></journpost><noarksak xmlns=\"\"><saId>68286</saId><saSaar>2014</saSaar><saSeknr>2703</saSeknr><saPapir>0</saPapir><saDato>2014-11-27</saDato><saTittel>Test Knutepunkt herokuapp</saTittel><saU1>0</saU1><saStatus>B</saStatus><saArkdel>EARKIV1</saArkdel><saType /><saJenhet>SENTRAL</saJenhet><saTgkode /><saUoff /><saBevtid /><saKasskode /><saKassdato /><saProsjekt /><saOfftittel>Test Knutepunkt herokuapp</saOfftittel><saAdmkort>FM-ADMA</saAdmkort><saAdmbet>Administrasjon</saAdmbet><saAnsvinit>JPS</saAnsvinit><saAnsvnavn>John Petter Svedal</saAnsvnavn><saTggruppnavn /></noarksak></Melding>";

        Document doc = DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder().newDocument();

        Element payLoadElement = doc.createElement("payload");
        Element base64dataElement = doc.createElement("data");
        base64dataElement.setTextContent(bestEDUMessage);
        payLoadElement.appendChild(base64dataElement);

        pmrt.setPayload(payLoadElement);

        new BinaryDataClient().send(pmrt, endPointURL);
        System.out.println("Send SOAP invocation done in " + (System.currentTimeMillis() - backThen) + " ms" );

    }

    public void send(PutMessageRequestType pmrt, String endpointURL) {

        System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");

        QName qname = new QName("http://www.arkivverket.no/Noark/Exchange", "noarkExchange");
        NoarkExchange service = new NoarkExchange(null, qname);
        SOAPport port = service.getNoarkExchangePort();
        BindingProvider bindingProvider = (BindingProvider) port;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointURL);
        port.putMessage(pmrt);
    }


    static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, "UTF-8");
    }

}
