package no.difi.messagehandler;

import eu.peppol.identifier.PeppolDocumentTypeId;
import no.difi.meldingsutveksling.domain.BestEduMessage;
import no.difi.meldingsutveksling.domain.SBD;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import eu.peppol.PeppolMessageMetaData;

import no.difi.messagehandler.peppolmessageutils.ProcessStates;
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;
import org.w3c.dom.*;

import javax.swing.text.AbstractDocument;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.*;
import java.util.zip.ZipFile;

public abstract class MessageReceieverTemplate {

    private EventLog eventLog = EventLog.create();
    private ProcessStates processStates;

    abstract void sendLeveringskvittering();

    abstract void sendApningskvittering();

    public void receive(PeppolMessageMetaData metaData,Document document) throws ParserConfigurationException, JAXBException {

       Map documentElements=documentMapping(document);
       Node n=(Node)documentElements.get("DocumentIdentification");

        eventLog.log(new Event().setState(5));

        if (isSBD(n)) {
            sendLeveringskvittering();
            eventLog.log(new Event());

            // depkryptert payload (AES)
            ZipFile asicFile = getZipFileFromDocument(document);
            eventLog.log(new Event());

            // Signaturvalidering
            verifySignature(asicFile);
            eventLog.log(new Event());

            BestEduMessage bestEduMessage = getBestEduFromAsic(asicFile);
            senToNoark(bestEduMessage);
            eventLog.log(new Event());

            sendApningskvittering();
            eventLog.log(new Event());

        } else {
            // Dette er en kvittering
        }
    }

    /**
     * Maps out document elements as nodes
     * @param document sbd or best/edu
     * @return List of node extended objects
     * @throws JAXBException
     */
    private Map<String,? extends Node> documentMapping(Document document) throws JAXBException {

        Map list = new HashMap();
        NodeList sbdhNodes= document.getElementsByTagName("ns2:StandardBusinessDocumentHeader");
        Node sbdhElement = sbdhNodes.item(0);
        NodeList payloadNodes = document.getElementsByTagName("payload");
        NodeList childs=sbdhElement.getChildNodes();
        for(int i=0;i<childs.getLength();i++) {
            Node n=   childs.item(i);
            String name = n.getNodeName();
            if(name.contains("ns2:"))
                name= name.replace("ns2:","");
                list.put(name,n);
        }
        for(int i=0;i< payloadNodes.getLength();i++) {
            Node n=   payloadNodes.item(i);
            list.put("payload",n);
        }

        return list;
    }


    protected void senToNoark(BestEduMessage bestEduMessage) {
    }

    private BestEduMessage getBestEduFromAsic(ZipFile asicFile) {
        return null;
    }

    private void verifySignature(ZipFile asicFile) {
    }

    private ZipFile getZipFileFromDocument(Document document) {
        return null;
    }

    private boolean isSBD(Node node) {

        return node.getTextContent().contains("Sbd");
    }

}
