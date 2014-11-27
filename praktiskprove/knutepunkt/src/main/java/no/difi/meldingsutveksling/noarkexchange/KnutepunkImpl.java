package no.difi.meldingsutveksling.noarkexchange;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.eventlog.Event;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.eventlog.ProcessState;
import no.difi.meldingsutveksling.noarkexchange.schema.*;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.ws.BindingType;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

/**
 * This is the implementation of the wenbservice that case managenent systems supporting
 * the BEST/EDU stadard communicates with. The responsibility of this component is to
 * create, sign and encrypt a SBD message for delivery to a PEPPOL access point
 * <p/>
 * The access point for the recipient is looked up through ELMA and SMK, the certificates are
 * retrived through a MOCKED adress register component not yet imolemented in any infrastructure.
 * <p/>
 * <p/>
 * User: glennbech
 * Date: 31.10.14
 * Time: 15:26
 */


@WebService(portName = "NoarkExchangePort", serviceName = "noarkExchange", targetNamespace = "http://www.arkivverket.no/Noark/Exchange", wsdlLocation = "http://hardcodeme.not", endpointInterface = "no.difi.meldingsutveksling.noarkexchange.schema.SOAPport")
@BindingType("http://schemas.xmlsoap.org/wsdl/soap/http")
public class KnutepunkImpl implements SOAPport {

    @Resource
    private WebServiceContext context;

    private SendMessageTemplate template;

    @Override
    public GetCanReceiveMessageResponseType getCanReceiveMessage(@WebParam(name = "GetCanReceiveMessageRequest", targetNamespace = "http://www.arkivverket.no/Noark/Exchange/types", partName = "getCanReceiveMessageRequest") GetCanReceiveMessageRequestType getCanReceiveMessageRequest) {
        ServletContext servletContext =
                (ServletContext) context.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);

        EventLog eventLog = ctx.getBean(EventLog.class);
        eventLog.log(new Event()
                .setMessage(new XStream().toXML(getCanReceiveMessageRequest))
                .setProcessStates(ProcessState.CAN_RECEIVE_INVOKED)
                .setReceiver(getCanReceiveMessageRequest.getReceiver().getOrgnr()).setSender("NA"));

        GetCanReceiveMessageResponseType response = new GetCanReceiveMessageResponseType();
        response.setResult(true);

        return response;
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType putMessageRequest) {

        ServletContext servletContext =
                (ServletContext) context.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        template = ctx.getBean(SendMessageTemplate.class);
        return template.sendMessage(putMessageRequest);
    }

    public WebServiceContext getContext() {
        return context;
    }

    public void setContext(WebServiceContext context) {
        this.context = context;
    }
}
