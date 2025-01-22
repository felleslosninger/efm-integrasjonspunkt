package no.difi.meldingsutveksling.cucumber;

import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultNamespacePrefixMapper extends NamespacePrefixMapper {

    private Map<String, String> namespaceMap = new HashMap<>();

    DefaultNamespacePrefixMapper() {
        namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        namespaceMap.put("http://www.altinn.no/services/2009/10", "altinn1");
        namespaceMap.put("http://www.altinn.no/services/ServiceEngine/Broker/2015/06", "altinn2");
        namespaceMap.put("http://www.altinn.no/services/common/fault/2009/10", "altinn3");
        namespaceMap.put("http://schemas.microsoft.com/2003/10/Serialization/", "ms");
        namespaceMap.put("http://schemas.altinn.no/services/serviceEntity/2015/06", "altinn4");

        namespaceMap.put("http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader", "dpi1");
        namespaceMap.put("http://www.w3.org/2000/09/xmldsig#", "dpi2");
        namespaceMap.put("http://begrep.difi.no/sdp/schema_v10", "dpi3");

        namespaceMap.put("http://schemas.altinn.no/services/Intermediary/Receipt/2009/10", "altinn5");
        namespaceMap.put("http://www.altinn.no/services/ServiceEngine/ReporteeElementList/2009/10", "altinn6");
        namespaceMap.put("http://schemas.altinn.no/services/ServiceEngine/Correspondence/2013/06", "altinn7");
        namespaceMap.put("http://schemas.altinn.no/serviceengine/formsengine/2009/10", "altinn8");
        namespaceMap.put("http://www.altinn.no/services/ServiceEngine/Correspondence/2009/10", "altinn9");
        namespaceMap.put("http://schemas.altinn.no/services/ServiceEngine/Correspondence/2010/10", "altinn10");
        namespaceMap.put("http://www.altinn.no/services/ServiceEngine/ReporteeElementList/2010/10", "altinn11");
        namespaceMap.put("http://schemas.altinn.no/services/ServiceEngine/Correspondence/2009/10", "altinn12");
        namespaceMap.put("http://schemas.altinn.no/services/ServiceEngine/Notification/2009/10", "altinn13");
        namespaceMap.put("http://schemas.altinn.no/services/ServiceEngine/Correspondence/2016/02", "altinn14");
        namespaceMap.put("http://schemas.altinn.no/services/ServiceEngine/Correspondence/2014/10", "altinn15");
        namespaceMap.put("http://schemas.altinn.no/services/ServiceEngine/Correspondence/2013/11", "altinn16");
        namespaceMap.put("http://schemas.altinn.no/services/ServiceEngine/Broker/2015/06", "altinn17");
    }

    public Map<String, String> getPrefix2UrlMap() {
        return namespaceMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    /* (non-Javadoc)
     * Returning null when not found based on spec.
     * @see com.sun.xml.bind.marshaller.NamespacePrefixMapper#getPreferredPrefix(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        return namespaceMap.getOrDefault(namespaceUri, suggestion);
    }
}