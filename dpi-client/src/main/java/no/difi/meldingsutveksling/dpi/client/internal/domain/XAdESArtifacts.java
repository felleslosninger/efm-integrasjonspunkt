package no.difi.meldingsutveksling.dpi.client.internal.domain;

import lombok.Value;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Value
public class XAdESArtifacts {
    Document document;
    Element signableProperties;
    String signablePropertiesReferenceUri;
}