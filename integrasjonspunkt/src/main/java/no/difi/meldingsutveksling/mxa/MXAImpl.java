package no.difi.meldingsutveksling.mxa;

import no.difi.meldingsutveksling.core.EDUCoreService;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.mxa.schema.MXADelegate;
import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.ws.BindingType;
import java.io.StringReader;

import static no.difi.meldingsutveksling.mxa.MessageMarker.markerFrom;

@Component("mxaService")
@WebService(portName = "MXAPort", serviceName = "MXA", targetNamespace = "http://webservice.ws.altut.patent.siriusit.com/", endpointInterface = "no.difi.meldingsutveksling.mxa.schema.MXADelegate")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class MXAImpl implements MXADelegate {

    @Autowired
    private EDUCoreService coreService;

    private static final Logger log = LoggerFactory.getLogger(MXAImpl.class);
    private static final int SUCCESS = 0;
    private static final int INTERNAL_ERROR = 1;
    private static final int XML_PARSE_ERROR = 2;

    @Override
    public int submitMessage(@WebParam(name = "submitMessage") String arg0) {
        JAXBContext jaxbContext = null;
        Message msg = null;
        try {
            jaxbContext = JAXBContext.newInstance(Message.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            msg = (Message) unmarshaller.unmarshal(new StringReader(arg0));
        } catch (JAXBException e) {
            log.error("XML parse error", e);
            Audit.error("XML parse error", markerFrom(msg), e);
            return XML_PARSE_ERROR;
        }
        Audit.info("MXA message received", markerFrom(msg));

        try {
            coreService.queueMessage(msg);
        } catch (Exception e) {
            log.error("Internal error", e);
            Audit.error("Internal error", markerFrom(msg), e);
            return INTERNAL_ERROR;
        }

        return SUCCESS;
    }

}
