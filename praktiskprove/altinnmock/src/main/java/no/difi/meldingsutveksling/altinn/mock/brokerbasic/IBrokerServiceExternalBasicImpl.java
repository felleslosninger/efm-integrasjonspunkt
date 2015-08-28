package no.difi.meldingsutveksling.altinn.mock.brokerbasic;

import javax.jws.WebService;
import javax.xml.bind.JAXBElement;

@WebService(targetNamespace = "http://www.altinn.no/services/ServiceEngine/Broker/2015/06", endpointInterface="no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasic", portName = "BasicHttpBinding_IBrokerServiceExternalBasic")
public class IBrokerServiceExternalBasicImpl implements IBrokerServiceExternalBasic {
    @Override
    public void test() throws IBrokerServiceExternalBasicTestAltinnFaultFaultFaultMessage {
    }

    @Override
    public String initiateBrokerServiceBasic(String systemUserName, String systemPassword, BrokerServiceInitiation brokerServiceInitiation) throws IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage {
        return "123456789";
    }

    @Override
    public BrokerServiceAvailableFileList getAvailableFilesBasic(String systemUserName, String systemPassword, BrokerServiceSearch searchParameters) throws IBrokerServiceExternalBasicGetAvailableFilesBasicAltinnFaultFaultFaultMessage {
        ObjectFactory objectFactory = new ObjectFactory();
        BrokerServiceAvailableFileList result = objectFactory.createBrokerServiceAvailableFileList();

        BrokerServiceAvailableFile file1 = objectFactory.createBrokerServiceAvailableFile();
        file1.setFileReference("somefilereference");
        file1.setReceiptID(54321);
        file1.setFileStatus(BrokerServiceAvailableFileStatus.UPLOADED);

        BrokerServiceAvailableFile file2 = objectFactory.createBrokerServiceAvailableFile();
        file2.setFileReference("somefilereference");
        file2.setReceiptID(789654);
        file2.setFileStatus(BrokerServiceAvailableFileStatus.DOWNLOADED);

        result.getBrokerServiceAvailableFile().add(file1);
        result.getBrokerServiceAvailableFile().add(file2);

        return result;
    }
}
