package no.difi.meldingsutveksling.mail;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.EntityType;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.xml.transform.StringSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import java.io.File;
import java.io.IOException;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore("Manual test")
public class EduMailSenderTest {

    private ServiceRegistryLookup serviceRegistryLookup;
    private JAXBContext putMessageJaxbContext;
    private EduMailSender mailSender;
    private IntegrasjonspunktProperties props;

    @Before
    public void init() throws JAXBException {
        props = new IntegrasjonspunktProperties();
        props.setMail(new IntegrasjonspunktProperties.Mail());
        props.getMail().setSmtpHost("leikexcas13.difi.local");
        props.getMail().setReceiverAddress("martin.wam@difi.no");
        props.getMail().setSmtpPort("25");
        props.getMail().setEnableAuth("false");
        props.getMail().setUsername("");
        props.getMail().setPassword("");
        mailSender = new EduMailSender(props);

        putMessageJaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
        serviceRegistryLookup = mock(ServiceRegistryLookup.class);

        InfoRecord infoRecord = new InfoRecord("1234", "Foo", new EntityType("Organisasjonsledd", "ORGL"));
        when(serviceRegistryLookup.getInfoRecord(anyString())).thenReturn(infoRecord);
    }

    @Test
    public void testSend() throws JAXBException, IOException {
        PutMessageRequestType putMessage = createPutMessageCdataXml(FileUtils.readFileToString(new File
                ("src/test/resources/putmessage_test.xml")));
        mailSender.send(putMessage, "foo");
    }

    private PutMessageRequestType createPutMessageCdataXml(String payload) throws JAXBException {
        Unmarshaller unmarshaller = putMessageJaxbContext.createUnmarshaller();
        return unmarshaller.unmarshal(new StringSource((payload)), PutMessageRequestType.class).getValue();
    }
}
