package no.difi.meldingsutveksling.cucumber;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

import java.util.HashMap;
import java.util.Map;

public class DefaultNamespacePrefixMapper extends NamespacePrefixMapper {

    private Map<String, String> namespaceMap = new HashMap<>();

    DefaultNamespacePrefixMapper() {
        namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        namespaceMap.put("http://www.altinn.no/services/2009/10", "altinn1");
        namespaceMap.put("http://www.altinn.no/services/ServiceEngine/Broker/2015/06", "altinn2");
        namespaceMap.put("http://www.altinn.no/services/common/fault/2009/10", "altinn3");
        namespaceMap.put("http://schemas.microsoft.com/2003/10/Serialization/", "ms");
        namespaceMap.put("http://schemas.altinn.no/services/serviceEntity/2015/06", "altinn4");
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