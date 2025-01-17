package no.difi.meldingsutveksling.ks.svarut;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

import jakarta.xml.bind.JAXBElement;
import java.util.List;
import java.util.Set;

@Slf4j
public class SvarUtWebServiceClientImpl extends WebServiceGatewaySupport implements SvarUtWebServiceClient {

    @Override
    public String sendMessage(SvarUtRequest request) {

        final JAXBElement<SendForsendelseMedIdResponse> response = (JAXBElement<SendForsendelseMedIdResponse>) getWebServiceTemplate().marshalSendAndReceive(request.getEndPointURL(), request.getForsendelse());

        return response.getValue().getReturn();
    }

    @Override
    public String getForsendelseId(String uri, String eksternRef) {
        log.debug("No local forsendelseId mapping for messageId={}, performing lookup..", eksternRef);
        RetrieveForsendelseIdByEksternRef request = RetrieveForsendelseIdByEksternRef.builder().
                withEksternRef(eksternRef).build();

        ObjectFactory objectFactory = new ObjectFactory();
        final JAXBElement<RetrieveForsendelseIdByEksternRefResponse> response =
                (JAXBElement<RetrieveForsendelseIdByEksternRefResponse>)
                        getWebServiceTemplate().marshalSendAndReceive(uri,
                                objectFactory.createRetrieveForsendelseIdByEksternRef(request));
        List<String> responseReturn = response.getValue().getReturn();
        if (responseReturn == null || responseReturn.isEmpty()) {
            return null;
        } else {
            return responseReturn.get(0);
        }
    }

    @Override
    public ForsendelseStatus getForsendelseStatus(String uri, String forsendelseId) {
        RetrieveForsendelseStatus request = RetrieveForsendelseStatus.builder().withForsendelsesid(forsendelseId).build();

        final JAXBElement<RetrieveForsendelseStatusResponse> response =
                (JAXBElement<RetrieveForsendelseStatusResponse>)
                        getWebServiceTemplate().marshalSendAndReceive(uri, request);
        return response.getValue().getReturn();
    }

    public List<StatusResult> getForsendelseStatuser(String uri, Set<String> forsendelseIds) {
        RetrieveForsendelseStatuser request = RetrieveForsendelseStatuser.builder().withForsendelseider(forsendelseIds).build();

        final JAXBElement<RetrieveForsendelseStatuserResponse> response =
                (JAXBElement<RetrieveForsendelseStatuserResponse>)
                        getWebServiceTemplate().marshalSendAndReceive(uri, request);
        return response.getValue().getReturn();
    }

    public List<String> retreiveForsendelseTyper(String uri) {
        RetreiveForsendelseTyper request = RetreiveForsendelseTyper.builder().build();
        JAXBElement<RetreiveForsendelseTyper> wrapped = new ObjectFactory().createRetreiveForsendelseTyper(request);
        final JAXBElement<RetreiveForsendelseTyperResponse> response =
                (JAXBElement<RetreiveForsendelseTyperResponse>) getWebServiceTemplate().marshalSendAndReceive(uri, wrapped);
        return response.getValue().getReturn();
    }
}
