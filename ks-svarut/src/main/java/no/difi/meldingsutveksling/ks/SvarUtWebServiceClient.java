package no.difi.meldingsutveksling.ks;

import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBElement;

public class SvarUtWebServiceClient extends WebServiceGatewaySupport {
    public void sendMessage() {

        byte[] data = new byte[1024];

        final Dokument dokument = Dokument.builder().withData(new DataHandler(new ByteArrayDataSource(data, "pdf"))).build();

        final Mottaker mottaker = Organisasjon.builder().build();


        final Printkonfigurasjon printkonfigurasjon = Printkonfigurasjon.builder().build();
        Forsendelse forsendelse = Forsendelse.builder()
                .withTittel("Tittel")
                .withAvgivendeSystem("Avgivende system?")
                .withDokumenter(dokument)
                .withMottaker(mottaker)
                .withPrintkonfigurasjon(printkonfigurasjon)
                .withKrevNiva4Innlogging(true)
                .withKryptert(true).build();

        final ObjectFactory objectFactory = new ObjectFactory();
        final SendForsendelse sendForsendelse = SendForsendelse.builder().withForsendelse(forsendelse).build();

        final JAXBElement<SendForsendelseResponse> response = (JAXBElement<SendForsendelseResponse>) getWebServiceTemplate().marshalSendAndReceive(objectFactory.createSendForsendelse(sendForsendelse));
        response.getValue();

        //final ForsendelseStatus forsendelseStatus = (ForsendelseStatus) getWebServiceTemplate().marshalSendAndReceive(forsendelseId);
    }
}
