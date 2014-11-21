package no.difi.meldingsutveksling.oxalisexchange;

import no.difi.meldingsutveksling.adresseregmock.AdressRegisterFactory;
import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: glennbech
 * Date: 10.11.14
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */
class OxalisMessageReceiverTemplate extends MessageReceieverTemplate {
    private final String LEVERINGSKVITTERING="leveringskvittering";
    private final String MIME_TYPE="application/xml";

    @Override
    void sendLeveringskvittering(Map nodeList) {
        Dokumentpakker dokumentpakker = new Dokumentpakker();

        Node senderNode=(Node)nodeList.get("Sender");
        Node reciverNode=(Node)nodeList.get("Receiver");
        Node businessScopeNode=(Node) nodeList.get("BusinessScope");
        NodeList businessChildNodes = businessScopeNode.getChildNodes();
        Node instanceIdentifierNoden=businessChildNodes.item(1);
        String conversationId=instanceIdentifierNoden.getTextContent();
        String[] sendToAr=senderNode.getTextContent().split(":");
        String[] recievedByAr=reciverNode.getTextContent().split(":");
        String sendTo = sendToAr[1].trim();
        String recievedBy = recievedByAr[1].trim();
        Certificate certificate=(Certificate) AdressRegisterFactory.createAdressRegister().getCertificate(recievedBy);
        Noekkelpar noekkelpar = new Noekkelpar((PrivateKey)nodeList.get("privateKey"),certificate);
        Avsender.Builder avsenderBuilder = Avsender.builder(new Organisasjonsnummer(recievedBy), noekkelpar);
        Avsender avsender=avsenderBuilder.build();

        Mottaker mottaker = new Mottaker(new Organisasjonsnummer(sendTo),AdressRegisterFactory.createAdressRegister().getPublicKey(sendTo));
        ByteArrayImpl byteArray = new ByteArrayImpl(genererKvittering(nodeList, LEVERINGSKVITTERING),LEVERINGSKVITTERING,MIME_TYPE);
       // byte[] resultSbd = dokumentpakker.pakkDokumentISbd(byteArray, avsender, mottaker,"12345678-id");

    }

    private byte[] genererKvittering(Map nodeList,String kvitteringsType) {
        Node docId =(Node) nodeList.get("BusinessScope");
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        Document kvittering = null;
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            kvittering = documentBuilder.newDocument();
            org.w3c.dom.Element rootElement = kvittering.createElement(kvitteringsType);
            Node importedNode= kvittering.importNode(docId,true);
            rootElement.appendChild(importedNode);
            kvittering.appendChild(rootElement);

        } catch (ParserConfigurationException e) {
            //TODO:log exception
            e.printStackTrace();

        }
        return kvittering.toString().getBytes();

    }

    @Override
    void sendApningskvittering() {

    }

    @Override
    public void receive(Document document) throws GeneralSecurityException {
        super.receive(document);
    }


}
