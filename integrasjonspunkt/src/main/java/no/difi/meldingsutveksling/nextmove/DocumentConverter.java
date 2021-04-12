package no.difi.meldingsutveksling.nextmove;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.ObjectFactory;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Component
class DocumentConverter {

    private final JAXBContext ctx;
    private final ObjectFactory objectFactory;

    public DocumentConverter() {
        try {
            this.ctx = JAXBContextFactory.createContext(new Class[]{
                    StandardBusinessDocument.class,
                    ArkivmeldingMessage.class,
                    ArkivmeldingKvitteringMessage.class,
                    DpiDigitalMessage.class,
                    DpiPrintMessage.class,
                    EinnsynKvitteringMessage.class,
                    InnsynskravMessage.class,
                    PubliseringMessage.class,
                    StatusMessage.class}, null);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not initialize " + DocumentConverter.class, e);
        }

        this.objectFactory = new ObjectFactory();
    }

    public byte[] marshallToBytes(StandardBusinessDocument sbd) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            getMarshaller().marshal(objectFactory.createStandardBusinessDocument(sbd), output);
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not marshall " + sbd, e);
        }

        return output.toByteArray();
    }

    private Marshaller getMarshaller() {
        try {
            return ctx.createMarshaller();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not create marshaller for " + StandardBusinessDocument.class, e);
        }
    }

    public StandardBusinessDocument unmarshallFrom(byte[] bytes) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        StreamSource streamSource = new StreamSource(inputStream);

        try {
            return getUnmarshaller().unmarshal(streamSource, StandardBusinessDocument.class).getValue();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not unmarshall to " + StandardBusinessDocument.class, e);
        }
    }

    private Unmarshaller getUnmarshaller() {
        try {
            return ctx.createUnmarshaller();
        } catch (JAXBException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not create Unmarshaller for " + StandardBusinessDocument.class, e);
        }
    }
}
