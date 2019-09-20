package no.difi.meldingsutveksling.noarkexchange.websak;

import no.difi.meldingsutveksling.noarkexchange.PayloadUtil;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import org.modelmapper.ModelMapper;

import javax.xml.bind.JAXBElement;

public class PutMessageRequestMapper {
    public JAXBElement<no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType> mapFrom(PutMessageRequestType domain) {
        no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType r =
                new no.difi.meldingsutveksling.noarkexchange.websak.schema.PutMessageRequestType();

        ModelMapper mapper = new ModelMapper();
        mapper.map(domain, r);
        r.setPayload(PayloadUtil.payloadAsString(domain.getPayload()));

        return new no.difi.meldingsutveksling.noarkexchange.websak.schema.ObjectFactory().createPutMessageRequest(r);
    }
}
