package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.ks.svarut.ObjectFactory;
import no.difi.meldingsutveksling.ks.svarut.SendForsendelseMedId;
import no.difi.meldingsutveksling.ks.svarut.SvarUtRequest;
import no.difi.meldingsutveksling.ks.svarut.SvarUtWebServiceClient;
import org.mockito.ArgumentCaptor;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@RequiredArgsConstructor
public class SvarUtSteps {

    private final SvarUtWebServiceClient client;
    private final XMLMarshaller xmlMarshaller;
    private final SvarUtDataParser svarUtDataParser;
    private final Holder<Message> messageSentHolder;

    @After
    public void after() {
        messageSentHolder.reset();
    }

    @Then("^an upload to Fiks is initiated with:$")
    @SneakyThrows
    public void anUploadToFiksInitiatedWith(String body) {
        ArgumentCaptor<SvarUtRequest> captor = ArgumentCaptor.forClass(SvarUtRequest.class);
        verify(client, timeout(5000).times(1))
                .sendMessage(captor.capture());

        SvarUtRequest svarUtRequest = captor.getValue();

        messageSentHolder.set(svarUtDataParser.parse(svarUtRequest));

        SendForsendelseMedId sendForsendelseMedId = svarUtRequest.getForsendelse();
        sendForsendelseMedId.getForsendelse().getDokumenter()
                .forEach(p -> p.setData(new DataHandler(new ByteArrayDataSource("<!--encrypted content-->".getBytes(), "text/plain"))));

        String result = xmlMarshaller.masrshall(
                new ObjectFactory().createSendForsendelseMedId(sendForsendelseMedId));
        assertThat(result).isXmlEqualTo(body);
    }
}
