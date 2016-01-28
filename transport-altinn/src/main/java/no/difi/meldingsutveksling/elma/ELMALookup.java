package no.difi.meldingsutveksling.elma;

import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.vefa.peppol.common.api.EndpointNotFoundException;
import no.difi.vefa.peppol.common.model.DocumentIdentifier;
import no.difi.vefa.peppol.common.model.Endpoint;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.common.model.ProcessIdentifier;
import no.difi.vefa.peppol.common.model.TransportProfile;
import no.difi.vefa.peppol.lookup.LookupClient;
import no.difi.vefa.peppol.lookup.api.LookupException;
import no.difi.vefa.peppol.security.api.PeppolSecurityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for ELMA Lookup of Altinn EndPoints
 *
 * @author Glenn Bech
 */
@Component
public class ELMALookup {

    private static final ProcessIdentifier PROCESS_IDENTIFIER = new ProcessIdentifier("urn:www.difi.no:profile:meldingsutveksling:ver1.0");
    private static final DocumentIdentifier DOCUMENT_IDENTIFIER = new DocumentIdentifier("urn:no:difi:meldingsuveksling:xsd::Melding##urn:www.difi.no:meldingsutveksling:melding:1.0:extended:urn:www.difi.no:encoded:aes-zip:1.0::1.0");

    private LookupClient lookupClient;
    private TransportProfile transportProfile;

    @Autowired
    public ELMALookup(LookupClient lookupClient, TransportProfile transportProfile) {
        this.lookupClient = lookupClient;
        this.transportProfile = transportProfile;
    }

    public Endpoint lookup(String organisationNumber) throws LookupException {
        try {
            return lookupClient.getEndpoint(new ParticipantIdentifier(organisationNumber),
                    DOCUMENT_IDENTIFIER,
                    PROCESS_IDENTIFIER,
                    transportProfile);
        } catch (PeppolSecurityException | EndpointNotFoundException e) {
            throw new MeldingsUtvekslingRuntimeException();
        }
    }
}