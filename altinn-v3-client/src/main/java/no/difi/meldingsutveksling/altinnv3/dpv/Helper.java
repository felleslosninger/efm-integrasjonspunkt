package no.difi.meldingsutveksling.altinnv3.dpv;

import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import no.arkivverket.standarder.noark5.arkivmelding.Arkivmelding;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static no.difi.meldingsutveksling.NextMoveConsts.ARKIVMELDING_FILE;


@Component
@RequiredArgsConstructor
public class Helper {
    private final IntegrasjonspunktProperties props;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final ArkivmeldingUtil arkivmeldingUtil;


    public Optional<DpvSettings> getDpvSettings(NextMoveOutMessage msg) {
        if (msg.getBusinessMessage() instanceof ArkivmeldingMessage amMsg) {
            if (amMsg.getDpv() != null) {
                return Optional.of(amMsg.getDpv());
            }
        }
        if (msg.getBusinessMessage() instanceof DigitalDpvMessage ddMsg) {
            if (ddMsg.getDpv() != null) {
                return Optional.of(ddMsg.getDpv());
            }
        }
        return Optional.empty();
    }

    public Arkivmelding getArkivmelding(NextMoveOutMessage message, Map<String, BusinessMessageFile> fileMap) {
        BusinessMessageFile arkivmeldingFile = Optional.ofNullable(fileMap.get(ARKIVMELDING_FILE))
            .orElseThrow(() -> new CorrespondenceApiException("%s not found for message %s".formatted(ARKIVMELDING_FILE, message.getMessageId())));

        try {
            Resource resource = optionalCryptoMessagePersister.read(message.getMessageId(), arkivmeldingFile.getIdentifier());
            return arkivmeldingUtil.unmarshalArkivmelding(resource);
        } catch (JAXBException | IOException e) {
            throw new CorrespondenceApiException("Failed to get Arkivmelding", e);
        }
    }

    public String getSenderName(NextMoveOutMessage msg) {
        String orgnr = SBDUtil.getPartIdentifier(msg.getSbd())
            .map(Iso6523::getOrganizationIdentifier)
            .orElse(props.getOrg().getNumber());
        return serviceRegistryLookup.getInfoRecord(orgnr).getOrganizationName();
    }

    public ServiceRecord getServiceRecord(NextMoveOutMessage message) {
        ServiceRecord serviceRecord;
        try {
            serviceRecord = serviceRegistryLookup.getServiceRecord(
                SRParameter.builder(message.getReceiverIdentifier())
                    .conversationId(message.getConversationId())
                    .process(message.getSbd().getProcess())
                    .build(),
                message.getSbd().getDocumentType());
        } catch (ServiceRegistryLookupException e) {
            throw new MeldingsUtvekslingRuntimeException("Could not get service record for receiver %s".formatted(message.getReceiverIdentifier()), e);
        }
        return serviceRecord;
    }
}
