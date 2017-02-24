package no.difi.meldingsutveksling.ks;

import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

import javax.xml.bind.JAXBElement;

public class SvarUtWebServiceClientImpl  extends WebServiceGatewaySupport implements SvarUtWebServiceClient {
    @Override
    public String sendMessage(SvarUtRequest request) {
        final SendForsendelse sendForsendelse = SendForsendelse.builder().withForsendelse(request.getForsendelse()).build();

        final JAXBElement<SendForsendelseResponse> response = (JAXBElement<SendForsendelseResponse>) getWebServiceTemplate().marshalSendAndReceive(request.getEndPointURL(), sendForsendelse);

        return response.getValue().getReturn();
    }

    @Override
    public ForsendelseStatus getForsendelseStatus(String uri, String forsendelseId) {
        RetrieveForsendelseStatus request = RetrieveForsendelseStatus.builder().withForsendelsesid(forsendelseId).build();
        final JAXBElement<RetrieveForsendelseStatusResponse> response = (JAXBElement<RetrieveForsendelseStatusResponse>) getWebServiceTemplate().marshalSendAndReceive(uri, request);
        return response.getValue().getReturn();
    }
}
