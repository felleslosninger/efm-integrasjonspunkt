package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.dokumentpakking.xml.Payload;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noark.NOARKSystem;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.*;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.annotation.Resource;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: glennbech
 * Date: 25.11.14
 * Time: 12:43
 * To change this template use File | Settings | File Templates.
 */
@WebService(portName = "ReceivePort", serviceName = "receive", targetNamespace = "", wsdlLocation = "file:/Users/glennbech/dev/meldingsutvikling-mellom-offentlige-virksomheter/praktiskprove/knutepunkt/src/main/webapp/WEB-INF/wsdl/knutepunktReceive.wsdl", endpointInterface = "no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class KnutePunktReceiveImpl implements SOAReceivePort {

    @Resource
    private WebServiceContext context;

    EventLog eventLog;

    public CorrelationInformation receive(@WebParam(name = "StandardBusinessDocument", targetNamespace = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader", partName = "receiveResponse") StandardBusinessDocument receiveResponse) {

        ServletContext servletContext =
                (ServletContext) context.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);

        //*** query to Elma to get PK
        List<Partner> senders = receiveResponse.getStandardBusinessDocumentHeader().getSender();
        Partner sender = senders.get(0);
        PartnerIdentification orgNr = sender.getIdentifier();
        String[] orgNrArr = orgNr.getValue().split(":");

        //*** get payload *****
        Payload payload = (Payload) receiveResponse.getAny();

        //*** get rsa cipher decrypt *****

        //*** get aes cipher decrypt *****
        try {
            Cipher aesCipher = Cipher.getInstance("AES");
            byte[] zipTobe = aesCipher.doFinal(null);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (BadPaddingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {

        }


        // Lage zip fil av byteArray

        // Ta ut "første entry" eller "entry basert på filnavn?", finne edu medling.

        // Best/EDU Melding er en PutMesssageRequestType - må gjøres om
        PutMessageRequestType mrt = new PutMessageRequestType();

        //*** Unmarshall xml*****
        PutMessageRequestType putMessageRequestType;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(PutMessageRequestType.class);
            Unmarshaller unMarshaller= jaxbContext.createUnmarshaller();
            //putMessageRequestType = (PutMessageRequestType) unMarshaller.unmarshal(new File());
        } catch (JAXBException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {

        }

        // Send the edu

        NOARKSystem noarkSystem = new NOARKSystem();
        noarkSystem.sendEduMeldig(new PutMessageRequestType());

        return new CorrelationInformation();
    }



}
