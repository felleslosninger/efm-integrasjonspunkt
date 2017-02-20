package no.difi.meldingsutveksling.noarkexchange;

import javax.xml.bind.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

public class TestData<T> {
    private final Class<T> classtype;
    private JAXBContext ctx;
    private Unmarshaller unmarshaller;

    public TestData(Class<T> classtype) throws JAXBException {
        this.classtype = classtype;
        ctx = JAXBContext.newInstance(classtype);
        unmarshaller = ctx.createUnmarshaller();
        unmarshaller.setEventHandler(new ValidationEventHandler() {
            @Override
            public boolean handleEvent(ValidationEvent event) {
                throw new RuntimeException(event.getMessage(), event.getLinkedException());
            }
        });
    }

    public T loadFromClasspath(String fileName) throws JAXBException, XMLStreamException {
        InputStream file = this.getClass().getClassLoader().getResourceAsStream(fileName);

        XMLInputFactory xif = XMLInputFactory.newFactory();
        XMLStreamReader xsr = xif.createXMLStreamReader(file);
        xsr.nextTag(); // Advance to Envelope tag
        xsr.nextTag(); // Advance to Body tag
        xsr.nextTag(); // Advance to getNumberResponse tag
        return unmarshaller.unmarshal(xsr, classtype).getValue();
    }
}
