package no.difi.meldingsutveksling.noarkexchange.ephorte;

import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.modelmapper.ModelMapper;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

public class PutMessageRequestMapper {
    public JAXBElement<no.difi.meldingsutveksling.noarkexchange.ephorte.schema.PutMessageRequestType> mapFrom(PutMessageRequestType domain) throws JAXBException {
        no.difi.meldingsutveksling.noarkexchange.ephorte.schema.PutMessageRequestType r =
                new no.difi.meldingsutveksling.noarkexchange.ephorte.schema.PutMessageRequestType();

        ModelMapper mapper = new ModelMapper();
        mapper.map(domain, r);
        r.setPayload(PayloadUtil.payloadAsString(domain.getPayload()));

        return new no.difi.meldingsutveksling.noarkexchange.ephorte.schema.ObjectFactory().createPutMessageRequest(r);
    }
}
