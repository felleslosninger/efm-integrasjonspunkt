package no.difi.meldingsutveksling.ptv;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.LogstashMarker;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

@Slf4j
@UtilityClass
class XMLUtil {

    static String asString(Source source, LogstashMarker logMarkers) {
        StringWriter sw = new StringWriter();
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StreamResult sr = new StreamResult(sw);
            transformer.transform(source, sr);
            return sw.toString();
        } catch (TransformerException e) {
            log.error(logMarkers, "Failed to marshall webservice response to XML string", e);
        }
        return "";
    }
}
