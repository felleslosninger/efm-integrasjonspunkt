package no.difi.meldingsutveksling.oxalisexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.Dokumentpakker;
import no.difi.meldingsutveksling.domain.*;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.services.AdresseregisterRest;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;


public class OxalisMessageReceiverTemplate extends MessageReceieverTemplate {
    private static final String LEVERINGSKVITTERING = "leveringskvittering";
    private static final String AAPNINGSKVITTERING = "aapningskvittering";
    private static final String MIME_TYPE = "application/xml";
    private static final String WRITE_TO = System.getProperty("user.home") + File.separator + "testToRemove" + File.separator + "somethingSbd.xml";
    private static final int INSTANCEIDENTIFIER_FIELD = 3;
    private static final String KVITTERING = "Kvittering";
    private EventLog eventLog = EventLog.create();

    @Autowired
    private IntegrasjonspunktNokkel integrasjonspunktNokkel;

    @Autowired
    private AdresseregisterRest adresseRegisterClient;

    @Override
    void sendLeveringskvittering(Map nodeList) {
        forberedKvitering(nodeList, LEVERINGSKVITTERING);
    }

    @Override
    void sendApningskvittering(Map nodeList) {
        forberedKvitering(nodeList, AAPNINGSKVITTERING);
    }

    private void forberedKvitering(Map nodeList, String kvitteringsType) {
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
        Certificate certificate = adresseRegisterClient.getCertificate(recievedBy);
        Noekkelpar noekkelpar = new Noekkelpar((PrivateKey) nodeList.get("privateKey"), certificate);
        Avsender.Builder avsenderBuilder = Avsender.builder(new Organisasjonsnummer(recievedBy), noekkelpar);
        Avsender avsender = avsenderBuilder.build();
        Mottaker mottaker = new Mottaker(new Organisasjonsnummer(sendTo), (X509Certificate) adresseRegisterClient.getCertificate(sendTo));
        ByteArrayImpl byteArray = new ByteArrayImpl(genererKvittering(nodeList, kvitteringsType), kvitteringsType.concat(".xml"), MIME_TYPE);

        try {
            Dokumentpakker dokumentpakker = new Dokumentpakker(integrasjonspunktNokkel.getSignatureHelper());
            byte[] resultSbd = dokumentpakker.pakkTilByteArray(byteArray, avsender, mottaker, instanceIdentifier, KVITTERING);
            File file = new File(WRITE_TO);
            FileUtils.writeByteArrayToFile(file, resultSbd);
        } catch (IOException e) {
            eventLog.log(new Event().setExceptionMessage(e.toString()));
            throw new MeldingsUtvekslingRuntimeException(e);
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
            eventLog.log(new Event().setExceptionMessage(e.getMessage()));
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        DOMSource source = new DOMSource(kvittering);
        StreamResult result = new StreamResult(baos);

        try {
            transformer = transformerFactory.newTransformer();
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        } catch (TransformerException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }

        return baos.toByteArray();

    }


}
