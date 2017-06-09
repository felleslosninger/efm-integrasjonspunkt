package no.difi.meldingsutveksling.ks;

import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

import javax.xml.bind.JAXBElement;

public class SvarUtWebServiceClientImpl extends WebServiceGatewaySupport implements SvarUtWebServiceClient {
    @Override
    public String sendMessage(SvarUtRequest request) {
        final SendForsendelse sendForsendelse = SendForsendelse.builder().withForsendelse(request.getForsendelse()).build();

        final JAXBElement<SendForsendelseResponse> response = (JAXBElement<SendForsendelseResponse>) getWebServiceTemplate().marshalSendAndReceive(request.getEndPointURL(), sendForsendelse);

        return response.getValue().getReturn();
    }

    @Override
    public String getForsendelseId(String uri, String eksternRef) {

        RetrieveForsendelseIdByEksternRef request = RetrieveForsendelseIdByEksternRef.builder().
                withEksternRef(eksternRef).build();

        ObjectFactory objectFactory = new ObjectFactory();
        final JAXBElement<RetrieveForsendelseIdByEksternRefResponse> response =
                (JAXBElement<RetrieveForsendelseIdByEksternRefResponse>)
                        getWebServiceTemplate().marshalSendAndReceive(uri,
                                objectFactory.createRetrieveForsendelseIdByEksternRef(request));
        return response.getValue().getReturn().get(0);
    }

    @Override
    public ForsendelseStatus getForsendelseStatus(String uri, String forsendelseId) {
        RetrieveForsendelseStatus request = RetrieveForsendelseStatus.builder().withForsendelsesid(forsendelseId).build();
        final JAXBElement<RetrieveForsendelseStatusResponse> response =
                (JAXBElement<RetrieveForsendelseStatusResponse>)
                        getWebServiceTemplate().marshalSendAndReceive(uri, request);
        return response.getValue().getReturn();
    }
}
