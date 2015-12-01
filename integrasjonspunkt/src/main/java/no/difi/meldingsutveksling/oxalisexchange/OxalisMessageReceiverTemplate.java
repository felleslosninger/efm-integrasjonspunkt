package no.difi.meldingsutveksling.oxalisexchange;

import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.services.AdresseregisterVirksert;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

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
    private AdresseregisterVirksert adresseRegisterClient;

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


    @Override
    void sendLeveringskvittering(Map list) {
        //todo
    }

    @Override
    void sendApningskvittering(Map list) {
        //todo
    }
}
