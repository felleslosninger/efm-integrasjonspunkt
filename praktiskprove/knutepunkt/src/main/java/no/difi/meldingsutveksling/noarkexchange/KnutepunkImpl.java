package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.noarkexchange.schema.*;
<<<<<<< HEAD
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
=======
>>>>>>> herokuhack

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
<<<<<<< HEAD
public class KnutepunkImpl implements SOAPport {

    @Resource
    private WebServiceContext context;

    SendMessageTemplate template;

    @Override
    public GetCanReceiveMessageResponseType getCanReceiveMessage(@WebParam(name = "GetCanReceiveMessageRequest", targetNamespace = "http://www.arkivverket.no/Noark/Exchange/types", partName = "getCanReceiveMessageRequest") GetCanReceiveMessageRequestType getCanReceiveMessageRequest) {
        return null;
=======
public class KnutepunkImpl extends noarkExchange_NoarkExchangePortImpl {

    @Override
    public GetCanReceiveMessageResponseType getCanReceiveMessage(GetCanReceiveMessageRequestType getCanReceiveMessageRequest) {
        return super.getCanReceiveMessage(getCanReceiveMessageRequest);
>>>>>>> herokuhack
    }

    @Override
    public PutMessageResponseType putMessage(PutMessageRequestType putMessageRequest) {
<<<<<<< HEAD

        ServletContext servletContext =
                (ServletContext) context.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
        ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);

        template = ctx.getBean(SendMessageTemplate.class);
        System.out.println(template.getClass().getName());
=======
        SendMessageTemplate template = new LogToEventLogOnlySendMessageTemplate();
>>>>>>> herokuhack
        return template.sendMessage(putMessageRequest);
    }

    public WebServiceContext getContext() {
        return context;
    }

    public void setContext(WebServiceContext context) {
        this.context = context;
    }
}
