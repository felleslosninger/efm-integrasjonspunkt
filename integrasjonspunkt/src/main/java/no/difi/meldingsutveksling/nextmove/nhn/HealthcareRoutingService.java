package no.difi.meldingsutveksling.nextmove.nhn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.HealthcareValidationException;
import no.difi.meldingsutveksling.nextmove.Dialogmelding;
import no.difi.meldingsutveksling.nextmove.DialogmeldingOut;
import no.difi.meldingsutveksling.nextmove.v2.Participant;
import no.difi.meldingsutveksling.nextmove.v2.ServiceRecordProvider;
import no.difi.meldingsutveksling.nhn.adapter.crypto.EncryptionException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthcareRoutingService {

    private final ServiceRecordProvider serviceRecordProvider;
    private final IntegrasjonspunktProperties properties;
    private final BusinessMessageEncryptionService businessMessageEncryptionService;
    @Value("${difi.move.feature.enableDPH:false}")
    private Boolean dphEnabled;


    public void validateAndApply(StandardBusinessDocument sbd) {
        if (!dphEnabled){
            log.error("Attempting to process Healthcare request when feature is disabled.");
            throw new HealthcareValidationException("Can not process request. Healthcare feature is disabled.");
        }
        withScopeValidation(sbd, doc -> {
            validate(sbd);
            applyRouting(sbd);
            encryptBusinessMessage(sbd);
        });
    }


    private void requireAtMostOneScopePerType(StandardBusinessDocument sbd, ScopeType scopeType) {
        if (sbd.getScopes().stream().filter(t -> t.getType().equals(scopeType.getFullname())).count() > 1) {
            throw new HealthcareValidationException("Only one " + scopeType.getFullname() + " is allowed.");
        }
    }

    private void requireOneScopePerType(StandardBusinessDocument sbd, ScopeType scopeType) {
        if (sbd.getScopes().stream().filter(t -> t.getType().equals(scopeType.getFullname())).count() != 1) {
            throw new HealthcareValidationException("Only one " + scopeType.getFullname() + " is allowed.");
        }
    }

    private void withScopeValidation(StandardBusinessDocument sbd,
                                     Consumer<StandardBusinessDocument> block) {
        requireAtMostOneScopePerType(sbd, ScopeType.RECEIVER_HERID2);
        requireAtMostOneScopePerType(sbd, ScopeType.SENDER_HERID2);
        requireAtMostOneScopePerType(sbd, ScopeType.SENDER_HERID1);
        requireAtMostOneScopePerType(sbd, ScopeType.RECEIVER_HERID1);

        block.accept(sbd);

        requireOneScopePerType(sbd, ScopeType.RECEIVER_HERID2);
        requireOneScopePerType(sbd, ScopeType.SENDER_HERID2);
        requireOneScopePerType(sbd, ScopeType.SENDER_HERID1);
        requireOneScopePerType(sbd, ScopeType.RECEIVER_HERID1);
    }


    private void validate(StandardBusinessDocument sbd) {
        ServiceRecord srReceiver = serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER);
        ServiceRecord srSender = serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER);

        if (!(sbd.getSenderIdentifier() instanceof NhnIdentifier)) {
            throw new HealthcareValidationException("Not able to construct sender identifier for document type: " + sbd.getDocumentType());
        }

        if (!(sbd.getReceiverIdentifier() instanceof NhnIdentifier receiverIdentifier)) {
            throw new HealthcareValidationException("Not able to construct identifier for document type: " + sbd.getDocumentType());
        }
        sbd.getScope(ScopeType.RECEIVER_HERID2).ifPresent(t -> {
            if (!Objects.equals(t.getInstanceIdentifier(), srReceiver.getHerIdLevel2()))
                throw new HealthcareValidationException("Receiver HerId 2 does not match information from address register.");
        });

        if (receiverIdentifier.isNhnPartnerIdentifier()) {
            var receiverOrgnummer = receiverIdentifier.getIdentifier();
            if (!Objects.equals(receiverOrgnummer, srReceiver.getOrganisationNumber())) {
                throw new HealthcareValidationException("Receiver organisation number does not match address register.");
            }
        }

        var isMultitenantSetup = properties.getDph().getAllowMultitenancy();
        if (!isMultitenantSetup) {
            if (properties.getDph().getWhitelistOrgnum().size() != 1) {
                throw new HealthcareValidationException("Multitenancy configuration error. Only one organisation number should be whitelisted.");
            }

            if (!Objects.equals(sbd.getSenderIdentifier().getIdentifier(), srSender.getOrganisationNumber()))
                throw new HealthcareValidationException("Multitenancy is not supported. Sender organisation number:" + sbd.getSenderIdentifier().getIdentifier() + " is not registered in AR.");
            if (!Objects.equals(sbd.getSenderIdentifier().getIdentifier(), properties.getDph().getWhitelistOrgnum().getFirst()))
                throw new HealthcareValidationException("Multitenancy is not supported. Sender organisation number:" + sbd.getSenderIdentifier().getIdentifier() + " is not allowed to send in.");


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

    private void encryptBusinessMessage(StandardBusinessDocument sbd) {
        ServiceRecord srReceiver = serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER);
        
            var dialogmelding = sbd.getBusinessMessage(Dialogmelding.class).get();

            if (dialogmelding.getResponsibleHealthcareProfessionalId()==null) {
                dialogmelding.setResponsibleHealthcareProfessionalId(((NhnIdentifier)sbd.getReceiverIdentifier()).getHerId2());
            }

            DialogmeldingOut.DialogmeldingOutBuilder outMessageBuilder = DialogmeldingOut.builder()
                .notat(dialogmelding.getNotat())
                .vedleggBeskrivelse(dialogmelding.getVedleggBeskrivelse())
                .responsibleHealthcareProfessionalId(((NhnIdentifier)sbd.getReceiverIdentifier()).getHerId2());
            try {
                sbd.setAny(businessMessageEncryptionService.encrypt(dialogmelding, srReceiver.getPemCertificate()));
            } catch (EncryptionException e) {
                throw new HealthcareValidationException("Not able to encrypt business message");
            }
    }

    private void applyRouting(StandardBusinessDocument sbd) {
        ServiceRecord srReceiver = serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER);
        ServiceRecord srSender = serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER);

        sbd.getScope(ScopeType.SENDER_HERID1).ifPresentOrElse(t -> {
                    if (!Objects.equals(t.getInstanceIdentifier(), srSender.getHerIdLevel1()))
                        throw new HealthcareValidationException("Incoming Sender HerID 1 does not match Adressregister information for HerID level 1 " + t.getInstanceIdentifier() + "and identifier " + sbd.getSenderIdentifier().getIdentifier());
                }, () ->
                        sbd.getScopes().add(new Scope().setType(ScopeType.SENDER_HERID1.getFullname()).setInstanceIdentifier(srSender.getHerIdLevel1()))

        );

        sbd.getScope(ScopeType.RECEIVER_HERID1).ifPresentOrElse(scope -> {
                    if (!scope.getInstanceIdentifier().equals(srReceiver.getHerIdLevel1())) {
                        throw new HealthcareValidationException("Incoming HerID does not match expected HERID level 1!");
                    }
                },
                () -> sbd.getScopes().add(new Scope().setType(ScopeType.RECEIVER_HERID1.getFullname()).setInstanceIdentifier(srReceiver.getHerIdLevel1()))
        );

        sbd.getScope(ScopeType.RECEIVER_HERID2).ifPresentOrElse(scope -> {
                    if (!Objects.equals(scope.getInstanceIdentifier(), srReceiver.getHerIdLevel2()))
                        throw new HealthcareValidationException("Incoming Receiver HerID 2 does not match Adressregister information for HerID level 2 " + scope.getInstanceIdentifier() + " and identifier " + sbd.getReceiverIdentifier().getIdentifier());

                }, () -> sbd.getScopes().add(new Scope().setType(ScopeType.RECEIVER_HERID2.getFullname()).setInstanceIdentifier(srReceiver.getHerIdLevel2()))
        );
    }
}
