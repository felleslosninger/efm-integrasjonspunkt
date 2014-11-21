package no.difi.meldingsutveksling.oxalisexchange;

import no.difi.meldingsutveksling.adresseregmock.AdressRegisterFactory;
import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;
import no.difi.meldingsutveksling.domain.Noekkelpar;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
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
    private static final String LEVERINGSKVITTERING = "leveringskvittering";
    private static final String AAPNINGSKVITTERING = "aapningskvittering";
    private static final String MIME_TYPE = "application/xml";
    private static final String WRITE_TO = System.getProperty("user.home") + File.separator + "somethingSbd.xml";
    private static final int INSTANCEIDENTIFIER_FIELD = 3;
    private EventLog eventLog = EventLog.create();

    @Override
    void sendLeveringskvittering(Map nodeList) {
        forberedKvitering(nodeList, LEVERINGSKVITTERING);
    }

    @Override
    void sendApningskvittering(Map nodeList) {
        forberedKvitering(nodeList, AAPNINGSKVITTERING);
    }

    private void forberedKvitering(Map nodeList, String leveringskvittering) {
        Dokumentpakker dokumentpakker = new Dokumentpakker();
        Node senderNode = (Node) nodeList.get("Sender");
        Node reciverNode = (Node) nodeList.get("Receiver");
        Node businessScopeNode = (Node) nodeList.get("BusinessScope");
        NodeList businessChildNodes = businessScopeNode.getChildNodes();
        Node instanceIdentifierNoden = businessChildNodes.item(1);
        NodeList scopeChildNodes = instanceIdentifierNoden.getChildNodes();
        Node child = scopeChildNodes.item(INSTANCEIDENTIFIER_FIELD);
        String instanceIdentifier = child.getTextContent();
        String[] sendToAr = senderNode.getTextContent().split(":");
        String[] recievedByAr = reciverNode.getTextContent().split(":");
        String sendTo = sendToAr[1].trim();
        String recievedBy = recievedByAr[1].trim();
        Certificate certificate = (Certificate) AdressRegisterFactory.createAdressRegister().getCertificate(recievedBy);
        Noekkelpar noekkelpar = new Noekkelpar((PrivateKey) nodeList.get("privateKey"), certificate);
        Avsender.Builder avsenderBuilder = Avsender.builder(new Organisasjonsnummer(recievedBy), noekkelpar);
        Avsender avsender = avsenderBuilder.build();
        Mottaker mottaker = new Mottaker(new Organisasjonsnummer(sendTo), AdressRegisterFactory.createAdressRegister().getPublicKey(sendTo));
        ByteArrayImpl byteArray = new ByteArrayImpl(genererKvittering(nodeList, leveringskvittering), leveringskvittering, MIME_TYPE);
        byte[] resultSbd = dokumentpakker.pakkDokumentISbd(byteArray, avsender, mottaker, instanceIdentifier);
        File file = new File(WRITE_TO);
        try {
            FileUtils.writeByteArrayToFile(file, resultSbd);
        } catch (IOException e) {
            eventLog.log(new Event().setExceptionMessage(e));
        }
    }

    private byte[] genererKvittering(Map nodeList, String kvitteringsType) {
        Node docId = (Node) nodeList.get("BusinessScope");
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        Document kvittering = null;
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            kvittering = documentBuilder.newDocument();
            org.w3c.dom.Element rootElement = kvittering.createElement(kvitteringsType);
            Node importedNode = kvittering.importNode(docId, true);
            rootElement.appendChild(importedNode);
            kvittering.appendChild(rootElement);

        } catch (ParserConfigurationException e) {
           eventLog.log(new Event().setExceptionMessage(e));

        }
        return kvittering.toString().getBytes();

    }

}
