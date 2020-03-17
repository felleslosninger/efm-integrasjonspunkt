package no.difi.meldingsutveksling.ks.svarut

import lombok.extern.slf4j.Slf4j
import net.logstash.logback.marker.LogstashMarker
import net.logstash.logback.marker.Markers
import no.difi.meldingsutveksling.QueueInterruptException
import no.difi.meldingsutveksling.util.logger
import org.springframework.stereotype.Component
import org.springframework.ws.client.WebServiceClientException
import org.springframework.ws.client.support.interceptor.ClientInterceptor
import org.springframework.ws.context.MessageContext
import java.io.StringWriter
import javax.xml.transform.Source
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult

@Component
@Slf4j
class SvarUtFaultInterceptor : ClientInterceptor {

    val log = logger()
    private val logMarkers: LogstashMarker = Markers.append("service_identifier", "DPF")

    /**
     * Processes the incoming response fault. Called for response fault messages before payload handling in the [ ].
     *
     *
     * Note: Will only be called if this interceptor's [.handleRequest]  method has successfully completed.
     *
     * @param messageContext contains the outgoing request message
     * @return `true` to continue processing of the request interceptors; `false` to indicate
     * blocking of the request endpoint chain
     * @throws WebServiceClientException in case of errors
     * @see MessageContext.getResponse
     * @see org.springframework.ws.FaultAwareWebServiceMessage.hasFault
     */
    override fun handleFault(messageContext: MessageContext): Boolean {
        val soapFault = formatSoapFault(messageContext.response.payloadSource)
        log.error(logMarkers.and<LogstashMarker>(Markers.append("soap_fault", soapFault)), "Failed to send message");
        if (soapFault.contains("Duplikat") || soapFault.contains("Forsendelse med samme mottaker")) {
            throw QueueInterruptException(soapFault)
        }
        throw SoapFaultException("Failed to send message")
    }

    private fun formatSoapFault(source: Source): String {
        val transformer = TransformerFactory.newInstance().newTransformer()
        val sw = StringWriter()
        transformer.setOutputProperty("indent", "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        val sr = StreamResult(sw)
        try {
            transformer.transform(source, sr)
        } catch (e: TransformerException) {
            log.error(this.logMarkers, "Failed to marshall webservice response to XML string", e)
            return ""
        }
        return sw.toString()
    }

    override fun handleRequest(messageContext: MessageContext?): Boolean {
        return true
    }

    override fun handleResponse(messageContext: MessageContext?): Boolean {
        return true
    }

    /**
     * Callback after completion of request and response (fault) processing. Will be called on any outcome, thus
     * allows for proper resource cleanup.
     *
     *
     * Note: Will only be called if this interceptor's [.handleRequest]  method has successfully completed.
     *
     * @param messageContext contains both request and response messages, the response should contains a Fault
     * @param ex exception thrown on handler execution, if any
     * @throws WebServiceClientException in case of errors
     * @since 2.2
     */
    override fun afterCompletion(messageContext: MessageContext?, ex: Exception?) {
    }

    internal class SoapFaultException(msg: String) : WebServiceClientException(msg)
}