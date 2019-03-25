package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.en.And;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceAvailableFile;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceAvailableFileList;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasic;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamed;

import javax.activation.DataHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RequiredArgsConstructor
public class AltinnInSteps {

    private final IBrokerServiceExternalBasic iBrokerServiceExternalBasic;
    private final IBrokerServiceExternalBasicStreamed iBrokerServiceExternalBasicStreamed;
    private final AltinnZipFactory altinnZipFactory;
    private final Holder<Message> messageInHolder;

    @After
    public void after() {
        messageInHolder.reset();
    }

    @And("^Altinn sends the message$")
    @SneakyThrows
    public void altinnSendsTheMessage() {
        BrokerServiceAvailableFileList filesBasic = new BrokerServiceAvailableFileList();
        BrokerServiceAvailableFile file = new BrokerServiceAvailableFile();
        file.setFileReference("testMessage");
        file.setReceiptID(1);
        filesBasic.getBrokerServiceAvailableFile().add(file);

        given(iBrokerServiceExternalBasic.getAvailableFilesBasic(any(), any(), any()))
                .willReturn(filesBasic);

        DataHandler dh = mock(DataHandler.class);
        given(dh.getInputStream()).willReturn(
                altinnZipFactory.createAltinnZip(messageInHolder.get())
        );

        given(iBrokerServiceExternalBasicStreamed.downloadFileStreamedBasic(any(), any(), any(), any()))
                .willReturn(dh);
    }
}
