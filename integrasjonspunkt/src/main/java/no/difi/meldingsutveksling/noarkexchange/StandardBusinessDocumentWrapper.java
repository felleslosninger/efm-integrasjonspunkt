package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class StandardBusinessDocumentWrapper {

    private final StandardBusinessDocument document;

    public StandardBusinessDocumentWrapper(StandardBusinessDocument standardBusinessDocument) {
        this.document = standardBusinessDocument;
    }

    public String getSenderOrgNumber() {
        return document.getStandardBusinessDocumentHeader().getSender().get(0).getIdentifier().getValue().split(":")[1];
    }

    public String getReceiverOrgNumber() {
        return document.getStandardBusinessDocumentHeader().getReceiver().get(0).getIdentifier().getValue().split(":")[1];
    }

    public Payload getPayload() {
        if(document.getAny() instanceof Payload) {
            return (Payload) document.getAny();
        } else if(document.getAny() instanceof Node) {
            return unmarshallAnyElement(document.getAny());
        }  else {
            throw new MeldingsUtvekslingRuntimeException("Could not cast any element " + document.getAny() + " from " + StandardBusinessDocument.class + " to " + Payload.class);
        }
    }

    private Payload unmarshallAnyElement(Object any) {
        JAXBContext jaxbContextP;
        Unmarshaller unMarshallerP;
        Payload payload;
        try {
            jaxbContextP = JAXBContext.newInstance(Payload.class);
            unMarshallerP = jaxbContextP.createUnmarshaller();
            payload = unMarshallerP.unmarshal((org.w3c.dom.Node) any, Payload.class).getValue();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException(e);
        }
        return payload;
    }

}
