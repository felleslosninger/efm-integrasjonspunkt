package no.difi.meldingsutveksling.noarkexchange.websak;

import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.modelmapper.ModelMapper;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

public class PutMessageRequestMapper {
    public JAXBElement<no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType> mapFrom(PutMessageRequestType domain) throws JAXBException {
        no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType r =
                new no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType();

        // alt 1. Not working because Payload is missing @XmlRootElement
//        final Marshaller marshaller = jaxbContext.createMarshaller();
//        final StringWriter writer = new StringWriter();
//        marshaller.marshal((Node) domain.getPayload(), writer);
//        domain.setPayload(writer.toString());

        // alt 2. get the text content of node (works in Java)
        ModelMapper mapper = new ModelMapper();
        mapper.map(domain, r);
        r.setPayload(PayloadUtil.payloadAsString(domain.getPayload()));

        JAXBElement<no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType> websakrequest
                = new no.difi.meldingsutveksling.noarkexchange.websak.schema.ObjectFactory().createPutMessageRequest(r);

        return websakrequest;
    }
}
