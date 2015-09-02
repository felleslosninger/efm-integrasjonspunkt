package no.difi.meldingsutveksling.altinn.mock.brokerstreamed;

import javax.jws.WebService;
import javax.xml.ws.soap.MTOM;

@MTOM(enabled = true, threshold = 128)
@WebService(serviceName = "BrokerServiceExternalBasicStreamedSF", targetNamespace = "http://www.altinn.no/services/ServiceEngine/Broker/2015/06", portName = "BasicHttpBinding_IBrokerServiceExternalBasicStreamed", wsdlLocation = "WEB-INF/wsdl/BrokerServiceExternalBasicStreamed.wsdl", endpointInterface = "no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamed")
public class IBrokerServiceExternalBasicStreamedImpl implements IBrokerServiceExternalBasicStreamed {
    @Override
    public void test() throws IBrokerServiceExternalBasicStreamedTestAltinnFaultFaultFaultMessage {
    }

    @Override
    public ReceiptExternalStreamedBE uploadFileStreamedBasic(StreamedPayloadBasicBE parameters, String fileName, String reference, String reportee, String systemPassword, String systemUserName) throws IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage {
        return null;
    }

    @Override
    public byte[] downloadFileStreamedBasic(String systemUserName, String systemPassword, String fileReference, String reportee) throws IBrokerServiceExternalBasicStreamedDownloadFileStreamedBasicAltinnFaultFaultFaultMessage {
        return new byte[0];
    }
}
