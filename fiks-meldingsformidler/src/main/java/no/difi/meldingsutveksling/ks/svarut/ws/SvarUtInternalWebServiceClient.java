package no.difi.meldingsutveksling.ks.svarut.ws;

import jakarta.xml.bind.JAXBElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

import java.util.List;
import java.util.Set;

@Slf4j
@SuppressWarnings("unchecked")
class SvarUtInternalWebServiceClient extends WebServiceGatewaySupport {

    public String sendMessage(SendForsendelseMedId forsendelse) {
        final JAXBElement<SendForsendelseMedIdResponse> response = (JAXBElement<SendForsendelseMedIdResponse>) getWebServiceTemplate()
            .marshalSendAndReceive(forsendelse);
        return response.getValue().getReturn();
    }

    public String getForsendelseId(String eksternRef) {
        log.debug("No local forsendelseId mapping for messageId={}, performing lookup..", eksternRef);
        RetrieveForsendelseIdByEksternRef request = RetrieveForsendelseIdByEksternRef.builder().
            withEksternRef(eksternRef).build();

        ObjectFactory objectFactory = new ObjectFactory();
        final JAXBElement<RetrieveForsendelseIdByEksternRefResponse> response =
            (JAXBElement<RetrieveForsendelseIdByEksternRefResponse>)
                getWebServiceTemplate().marshalSendAndReceive(objectFactory.createRetrieveForsendelseIdByEksternRef(request));
        List<String> responseReturn = response.getValue().getReturn();
        if (responseReturn == null || responseReturn.isEmpty()) {
            return null;
        } else {
            return responseReturn.getFirst();
        }
    }

    public List<StatusResult> getForsendelseStatuser(Set<String> forsendelseIds) {
        RetrieveForsendelseStatuser request = RetrieveForsendelseStatuser.builder().withForsendelseider(forsendelseIds).build();

        final JAXBElement<RetrieveForsendelseStatuserResponse> response =
            (JAXBElement<RetrieveForsendelseStatuserResponse>)
                getWebServiceTemplate().marshalSendAndReceive(request);
        return response.getValue().getReturn();
    }

    public List<String> retreiveForsendelseTyper() {
        RetreiveForsendelseTyper request = RetreiveForsendelseTyper.builder().build();
        JAXBElement<RetreiveForsendelseTyper> wrapped = new ObjectFactory().createRetreiveForsendelseTyper(request);
        final JAXBElement<RetreiveForsendelseTyperResponse> response =
            (JAXBElement<RetreiveForsendelseTyperResponse>) getWebServiceTemplate().marshalSendAndReceive(wrapped);
        return response.getValue().getReturn();
    }
}
