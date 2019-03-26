package no.difi.meldingsutveksling.cucumber;

import lombok.SneakyThrows;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.xml.transform.StringResult;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

@Component
@Profile("cucumber")
public class XMLMarshaller {

    private static final String PREFIX_MAPPER = "com.sun.xml.bind.namespacePrefixMapper";

    @SneakyThrows
    <T> String masrshall(JAXBElement<T> t) {
        JAXBContext context = JAXBContext.newInstance(t.getDeclaredType());

        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(PREFIX_MAPPER, new DefaultNamespacePrefixMapper());

        StringResult result = new StringResult();
        marshaller.marshal(t, result);
        return result.toString();
    }
}
