package no.difi.meldingsutveksling.ks;

import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

public class SvarUtWebServiceClient extends WebServiceGatewaySupport {
    public String sendMessage(Forsendelse forsendelse) {



        final SendForsendelse sendForsendelse = SendForsendelse.builder().withForsendelse(forsendelse).build();

        final SendForsendelseResponse response = (SendForsendelseResponse) getWebServiceTemplate().marshalSendAndReceive(sendForsendelse);

        return response.getReturn();
    }

    public ForsendelseStatus getForsendelseStatus(String forsendelseId) {
        RetrieveForsendelseStatus request = RetrieveForsendelseStatus.builder().withForsendelsesid(forsendelseId).build();
        final RetrieveForsendelseStatusResponse response = (RetrieveForsendelseStatusResponse) getWebServiceTemplate().marshalSendAndReceive(request);
        return response.getReturn();
    }
}
