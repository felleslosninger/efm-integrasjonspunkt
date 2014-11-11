package no.difi.messagehandler;

import eu.peppol.PeppolMessageMetaData;
import no.difi.messagehandler.peppolmessageutils.PeppolMessageMetadata;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created with IntelliJ IDEA.
 * User: glennbech
 * Date: 10.11.14
 * Time: 10:45
 * To change this template use File | Settings | File Templates.
 */
public class OxalisMessageReceiverTemplate extends MessageReceieverTemplate {

    @Override
    void sendLeveringskvittering() {
    }

    @Override
    void sendApningskvittering() {

    }

}
