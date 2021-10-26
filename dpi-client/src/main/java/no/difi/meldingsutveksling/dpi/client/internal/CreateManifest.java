package no.difi.meldingsutveksling.dpi.client.internal;

import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import no.difi.meldingsutveksling.dpi.client.internal.domain.Manifest;
import no.difi.meldingsutveksling.dpi.client.sdp.SDPManifest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXParseException;

import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;

@Component
public class CreateManifest {

    private final Jaxb2Marshaller marshaller;
    private final SDPBuilder sdpBuilder;

    public CreateManifest(SDPBuilder sdpBuilder) throws java.lang.Exception {
        this.marshaller = createJaxb2Marshaller();
        this.sdpBuilder = sdpBuilder;
    }

    private Jaxb2Marshaller createJaxb2Marshaller() throws java.lang.Exception {
        Jaxb2Marshaller m = new Jaxb2Marshaller();
        m.setClassesToBeBound(SDPManifest.class);
        m.setSchema(Schemas.SDP_MANIFEST_SCHEMA);
        m.afterPropertiesSet();
        return m;
    }

    public Manifest createManifest(Shipment shipment) {
        SDPManifest sdpManifest = sdpBuilder.createManifest(shipment);
        ByteArrayOutputStream manifestStream = new ByteArrayOutputStream();
        try {
            marshaller.marshal(sdpManifest, new StreamResult(manifestStream));
            return new Manifest(new ByteArrayResource(manifestStream.toByteArray()));
        } catch (MarshallingFailureException e) {
            if (e.getMostSpecificCause() instanceof SAXParseException) {
                throw new Exception("Kunne ikke validere generert Manifest XML. Sjekk at alle påkrevde input er satt og ikke er null",
                        e.getMostSpecificCause());
            }

            throw e;
        }
    }

    private static class Exception extends RuntimeException {
        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
