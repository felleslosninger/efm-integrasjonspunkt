package no.difi.meldingsutveksling.altinn.mock.brokerstreamed;

import javax.jws.WebService;

@WebService(endpointInterface = "no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamed")
public class IBrokerServiceExternalBasicStreamedImpl implements IBrokerServiceExternalBasicStreamed {
    @Override
    public void test() throws IBrokerServiceExternalBasicStreamedTestAltinnFaultFaultFaultMessage {
    }

    @Override
    public ReceiptExternalStreamedBE uploadFileStreamedBasic(StreamedPayloadBasicBE parameters) throws IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage {
        return null;
    }

    @Override
    public byte[] downloadFileStreamedBasic(String systemUserName, String systemPassword, String fileReference, String reportee) throws IBrokerServiceExternalBasicStreamedDownloadFileStreamedBasicAltinnFaultFaultFaultMessage {
        return new byte[0];
    }
}
