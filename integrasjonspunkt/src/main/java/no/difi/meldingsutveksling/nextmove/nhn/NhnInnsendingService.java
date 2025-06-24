package no.difi.meldingsutveksling.nextmove.nhn;

import jakarta.servlet.http.HttpServletRequest;
import no.difi.meldingsutveksling.core.Sender;
import no.difi.meldingsutveksling.domain.sbdh.Authority;
import no.difi.meldingsutveksling.nextmove.v2.ServiceRecordProvider;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryClient;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.IdentifierResource;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.digipost.org.unece.cefact.namespaces.standardbusinessdocumentheader.Partner;
import no.digipost.org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NhnInnsendingService {

    private final ServiceRegistryClient serviceRegistryClient;

    @Autowired
    public NhnInnsendingService(ServiceRegistryClient serviceRecordProvider) {
        this.serviceRegistryClient = serviceRecordProvider;
    }

    public void sendInn(StandardBusinessDocument sbd, HttpServletRequest httpServletRequest) {
        Partner recievingPartner = sbd.getStandardBusinessDocumentHeader().getReceivers().stream().filter(reciever -> reciever.getIdentifier().getAuthority() == "FNR").findAny().orElse(null);
        //@TODO identifier kan vare HERID
        assert recievingPartner != null;
        SRParameter srParameter = SRParameter.builder(recievingPartner.getIdentifier().getValue()).build();
        try {
            // kanskje vi ikke trenger å hente ut HRID 1 og 2 men bare å validere at DPH service record finnes
            // men for øyeblikket kan vi anta at vi har ogsp Herid 1 og 2
            // siden vi bruker service registry bare for validation her vi kan tenke om det å opprette en validate endepunkt på SR siden istendenfor å hente ut hele Identifier resource
            //
            IdentifierResource resource = serviceRegistryClient.loadIdentifierResource(srParameter);
            ServiceRecord sr = resource.getServiceRecords().stream().filter(res ->
               res.getService().getIdentifier().getFullname() == "DPH"
            ).findAny().orElse(null);

            assert sr != null;
            String herIdNivo1 = sr.getService().getHerdId1();
            String herIdNivo2 = sr.getService().getHerId2();

           // vi trenger å validere Avsender i tilleg til reciever
            Partner sendingPartnerNivo2 = sbd.getStandardBusinessDocumentHeader().getSenders().stream().filter(sender -> sender.getIdentifier().getAuthority() == "HERID").findAny().orElse(null);
            Partner sendingPartnerNivo1 = sbd.getStandardBusinessDocumentHeader().getSenders().stream().filter(sender -> sender.getIdentifier().getAuthority() == Authority.ISO6523_ACTORID_UPIS).findAny().orElse(null);
            // validate sender nivå 1 og nivå 2 via Service registry

            // Alt OK time to send in


           // @TODO nå vbet vi at vi kan sende ut til reciever


        } catch (ServiceRegistryLookupException e) {
            throw new RuntimeException("Klarer ikke finne resource",e);
        }





       // serviceRegistryClient.loadIdentifierResource();



    }

}
