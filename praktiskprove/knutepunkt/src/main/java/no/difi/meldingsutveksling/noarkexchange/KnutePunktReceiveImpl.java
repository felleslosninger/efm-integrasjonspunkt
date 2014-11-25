package no.difi.meldingsutveksling.noarkexchange;

import com.thoughtworks.xstream.XStream;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.CorrelationInformation;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.SOAReceivePort;
import no.difi.meldingsutveksling.noarkexchange.schema.receive.StandardBusinessDocument;
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

        XStream xs = new XStream();
        System.out.println(xs.toXML(receiveResponse));
        return new CorrelationInformation();
    }
}
