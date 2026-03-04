package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.Dialogmelding;
import no.difi.meldingsutveksling.nextmove.Notat;
import no.difi.meldingsutveksling.nextmove.Person;
import no.difi.meldingsutveksling.nextmove.nhn.NhnAdapterClient;
import no.difi.meldingsutveksling.nhn.adapter.model.InMessage;
import no.difi.meldingsutveksling.nhn.adapter.model.SerializeableIncomingBusinessDocument;
import no.difi.meldingsutveksling.sbd.SBDFactory;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NhnNextMoveMessageInService {
    private final NhnAdapterClient nhnClient;
    private final SBDFactory  sbdFactory;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final static String nhnProcess="urn:no:difi:profile:digitalpost:helse:ver1.0";
    private final static String standardDocumentType = "urn:no:difi:digitalpost:json:schema::dialogmelding";


    public StandardBusinessDocument getMessageByHerId(Integer herId2,String onBehalfOf) throws ServiceRegistryLookupException {
        List<InMessage> incomingMessages = nhnClient.incomingMessages(herId2,onBehalfOf);
        if (!incomingMessages.isEmpty()) {
            var firstIncoming =  incomingMessages.getFirst();
            // to situasjoner her. Enten vi har eksisterende conversation og da kan vi derive process fra conversation
            // eller det er helt ny melding da er det ikke mulig si o det kommer fra fastlege eller en annen nhn registrert party
            // kanskje best i første omgang å anta at det kommer fra fastlegen.
            // potensielt man kan sjekke fastlegeregisteret om det er fastlege elker ikke
            var senderSr = serviceRegistryLookup.getServiceRecord(SRParameter.builder(firstIncoming.getSenderHerId()+"").process(nhnProcess).build(), standardDocumentType);
            var receiverSr = serviceRegistryLookup.getServiceRecord(SRParameter.builder(firstIncoming.getReceiverHerId()+"").process(nhnProcess).build(),standardDocumentType);
            NhnIdentifier receiverIdentifier = NhnIdentifier.of(receiverSr.getOrganisationNumber(),receiverSr.getHerIdLevel1(), receiverSr.getHerIdLevel2());
            NhnIdentifier  senderIdentifier = NhnIdentifier.of(senderSr.getOrganisationNumber(),senderSr.getHerIdLevel1(), senderSr.getHerIdLevel2());

            SerializeableIncomingBusinessDocument incomingDocument = nhnClient.incomingBusinessDocument(UUID.fromString(firstIncoming.getId()),onBehalfOf);
            String conversationId = incomingDocument.getConversationRef()!=null ? incomingDocument.getConversationRef().getRefToConversation() : UUID.randomUUID().toString();
            var notatFromBd = incomingDocument.getMessage().getNotat();
            var patient = incomingDocument.getReceiver().getPatient();
            Dialogmelding dialogmelding = new Dialogmelding(new Notat(notatFromBd.getTema(),notatFromBd.getInnhold()),patient.getFnr(),senderSr.getHerIdLevel2(),incomingDocument.getVedlegg().getDescription(), new Person(patient.getFnr(),patient.getFnr(),patient.getMiddleName(), patient.getLastName(),""));

            return sbdFactory.createNextMoveSBD(senderIdentifier,receiverIdentifier,conversationId,incomingDocument.getId(),nhnProcess,standardDocumentType ,dialogmelding);
        }
    return null;
    }

}
