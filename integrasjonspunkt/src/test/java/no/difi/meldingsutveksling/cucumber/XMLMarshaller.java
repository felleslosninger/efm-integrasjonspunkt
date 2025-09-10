package no.difi.meldingsutveksling.cucumber;

import lombok.SneakyThrows;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.xml.transform.StringResult;
import org.springframework.xml.transform.StringSource;
import org.w3c.dom.Document;

import jakarta.xml.bind.*;
import javax.xml.parsers.DocumentBuilderFactory;

@Component
@Profile("cucumber")
public class XMLMarshaller {

    static final String PREFIX_MAPPER = "org.glassfish.jaxb.namespacePrefixMapper";

    @SneakyThrows
    <T> String marshall(JAXBElement<T> t) {
        StringResult result = new StringResult();
        getMarshaller(t).marshal(t, result);
        return result.toString();
    }

    @SneakyThrows
    <T> Document getDocument(JAXBElement<T> t) {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        getMarshaller(t).marshal(t, document);
        return document;
    }

    private <T> Marshaller getMarshaller(JAXBElement<T> t) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(t.getDeclaredType());

        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(PREFIX_MAPPER, new DefaultNamespacePrefixMapper());
        return marshaller;
    }

    @SneakyThrows
    <T> T unmarshall(String s, Class<T> declaredType) {
        JAXBContext context = JAXBContext.newInstance(declaredType);

        Unmarshaller unmarshaller = context.createUnmarshaller();
        JAXBElement<T> element = unmarshaller.unmarshal(new StringSource(s), declaredType);
        return element.getValue();
    }
}
