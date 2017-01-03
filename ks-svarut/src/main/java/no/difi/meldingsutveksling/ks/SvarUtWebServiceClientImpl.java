package no.difi.meldingsutveksling.ks;

import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

public class SvarUtWebServiceClientImpl  extends WebServiceGatewaySupport implements SvarUtWebServiceClient {
    @Override
    public String sendMessage(Forsendelse forsendelse) {
        final SendForsendelse sendForsendelse = SendForsendelse.builder().withForsendelse(forsendelse).build();

        final SendForsendelseResponse response = (SendForsendelseResponse) getWebServiceTemplate().marshalSendAndReceive(sendForsendelse);

        return response.getReturn();
    }

    @Override
    public ForsendelseStatus getForsendelseStatus(String forsendelseId) {
        RetrieveForsendelseStatus request = RetrieveForsendelseStatus.builder().withForsendelsesid(forsendelseId).build();
        final RetrieveForsendelseStatusResponse response = (RetrieveForsendelseStatusResponse) getWebServiceTemplate().marshalSendAndReceive(request);
        return response.getReturn();
    }
}
