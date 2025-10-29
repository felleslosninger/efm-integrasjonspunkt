package no.difi.meldingsutveksling.nextmove.nhn;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.HealthcareValidationException;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.nextmove.v2.Participant;
import no.difi.meldingsutveksling.nextmove.v2.ServiceRecordProvider;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class HealthcareRoutingService {

    private final ServiceRecordProvider serviceRecordProvider;
    private final IntegrasjonspunktProperties properties;


    public void validateAndApply(StandardBusinessDocument sbd) {
        validate(sbd);
        applyRouting(sbd);
    }


    private void validate(StandardBusinessDocument sbd) {
        ServiceRecord srReciever = serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER);
        ServiceRecord srSender = serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER);

        if (!(sbd.getReceiverIdentifier() instanceof NhnIdentifier recieverIdentifier)) {
            throw new HealthcareValidationException("Not able to construct identifier for document type: " + sbd.getDocumentType());
        }


        if (recieverIdentifier.isNhnPartnerIdentifier()) {
            var recieverOrgnummer = recieverIdentifier.getIdentifier();
            if (!Objects.equals(recieverOrgnummer, srReciever.getOrganisationNumber())) {
                throw new HealthcareValidationException("Receiver organisation number does not match address register.");
            }
        }

        var isMultitenantSetup = properties.getDph().getAllowMultitenancy();
        if (!isMultitenantSetup) {
            var fromConfigurationHerID = properties.getDph().getSenderHerId1();

            if (!fromConfigurationHerID.equals(srSender.getHerIdLevel1()))
                throw new HealthcareValidationException("Multitenancy not supported: Routing information in message does not match Adressregister information for herID1" + properties.getDph().getSenderHerId1() + " and orgnum " + srSender.getOrganisationNumber());
            sbd.getScope(ScopeType.SENDER_HERID1).ifPresent(t -> {
                if (!Objects.equals(t.getInstanceIdentifier(), srSender.getHerIdLevel1()))
                    throw new HealthcareValidationException("Multitenancy not supported: Routing information in message does not match Adressregister information for HerID level 1 " + t.getInstanceIdentifier() + " and orgnum " + sbd.getSenderIdentifier().getIdentifier());
            });
            if (!Objects.equals(sbd.getSenderIdentifier().getIdentifier(), srSender.getOrganisationNumber()))
                throw new HealthcareValidationException("Multitenancy is not supported. Sender organisation number:" + sbd.getSenderIdentifier().getIdentifier() + " is not registered in AR ");


        } else {
            if (!properties.getDph().getWhitelistOrgnum()
                .contains(srSender.getOrganisationNumber())) {
                throw new HealthcareValidationException("Sender not allowed " + srSender.getOrganisationNumber());
            }
            if (!sbd.getSenderIdentifier().getIdentifier().equals(srSender.getOrganisationNumber())) {
                throw new HealthcareValidationException("Sender information does not match Adressregister information.");
            }
        }
    }

    private void applyRouting(StandardBusinessDocument sbd) {
        ServiceRecord srReceiver = serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER);
        ServiceRecord srSender = serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER);

        if (sbd.getScope(ScopeType.SENDER_HERID1).isEmpty()) {
            sbd.getScopes().add(new Scope().setType(ScopeType.SENDER_HERID1.getFullname()).setInstanceIdentifier(srSender.getHerIdLevel1()));
        }
        if (sbd.getScope(ScopeType.SENDER_HERID2).isEmpty()) {
            sbd.getScopes().add(new Scope().setType(ScopeType.SENDER_HERID2.getFullname()).setInstanceIdentifier(srSender.getHerIdLevel2()));
        }
        if (sbd.getScope(ScopeType.RECEIVER_HERID2).isEmpty()) {
            sbd.getScopes().add(new Scope().setType(ScopeType.RECEIVER_HERID2.getFullname()).setInstanceIdentifier(srReceiver.getHerIdLevel2()));
        }
        if ( sbd.getScope(ScopeType.RECEIVER_HERID1).isPresent()) {
            if (!sbd.getScope(ScopeType.RECEIVER_HERID1).get().getInstanceIdentifier().equals(srReceiver.getHerIdLevel1())) {
                throw new HealthcareValidationException("Incoming HerID does not match expected HERID level 1!");
            }
        } else {
            sbd.getScopes().add(new Scope().setType( ScopeType.RECEIVER_HERID1.getFullname()).setInstanceIdentifier(srReceiver.getHerIdLevel1()));
        }
    }
}
