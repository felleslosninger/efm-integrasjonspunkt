package no.difi.meldingsutveksling.transport;


import eu.peppol.outbound.OxalisOutboundModule;
import eu.peppol.outbound.transmission.TransmissionRequest;
import eu.peppol.outbound.transmission.TransmissionRequestBuilder;
import eu.peppol.outbound.transmission.Transmitter;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import static no.difi.meldingsutveksling.dokumentpakking.xml.MarshalSBD.marshal;

/**
 * Oxalis implementation of the trasnport interface. Uses the oxalis outbound module to transmit the SBD
 *
 * @author Glenn Bech
 */
public class OxalisTransport implements Transport {

    @Override
    public void send(StandardBusinessDocument document) {
        document.getStandardBusinessDocumentHeader().getBusinessScope().getScope().addAll(createOxalisSpecificScopes());

        OxalisOutboundModule oxalisOutboundModule = new OxalisOutboundModule();

        TransmissionRequestBuilder requestBuilder = oxalisOutboundModule.getTransmissionRequestBuilder();

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        marshal(document, os);

        requestBuilder.payLoad(new ByteArrayInputStream(os.toByteArray()));
        TransmissionRequest transmissionRequest = requestBuilder.build();

        Transmitter transmitter = oxalisOutboundModule.getTransmitter();
        transmitter.transmit(transmissionRequest);
    }

    private List<Scope> createOxalisSpecificScopes() {
        List<Scope> scopes = new ArrayList<Scope>();
        scopes.add(createScope("DOCUMENTID", null,
                "urn:no:difi:meldingsuveksling:xsd::Melding##urn:www.difi.no:meldingsutveksling:melding:1.0:extended:urn:www.difi.no:encoded:aes-zip:1.0::1.0"));
        scopes.add(createScope("PROCESSID", null, "urn:www.difi.no:profile:meldingsutveksling:ver1.0"));
        return scopes;
    }

    private Scope createScope(String type, String identifier, String instanceIdentifier) {
        Scope scope = new Scope();

        scope.setIdentifier(identifier);
        scope.setInstanceIdentifier(instanceIdentifier);
        scope.setType(type);

        return scope;
    }

}
