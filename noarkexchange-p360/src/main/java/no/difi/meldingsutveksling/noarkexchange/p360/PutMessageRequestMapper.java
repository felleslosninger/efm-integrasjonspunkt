package no.difi.meldingsutveksling.noarkexchange.p360;

import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.modelmapper.ModelMapper;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

public class PutMessageRequestMapper {
    public JAXBElement<no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType> mapFrom(PutMessageRequestType domain) throws JAXBException {
        no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType r =
                new no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType();

        ModelMapper mapper = new ModelMapper();
        mapper.map(domain, r);

        JAXBElement<no.difi.meldingsutveksling.noarkexchange.p360.schema.PutMessageRequestType> p360request
                = new no.difi.meldingsutveksling.noarkexchange.p360.schema.ObjectFactory().createPutMessageRequest(r);

        return p360request;
    }
}
