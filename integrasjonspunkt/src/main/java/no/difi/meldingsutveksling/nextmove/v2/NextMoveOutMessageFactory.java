package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.exceptions.UnknownMessageTypeException;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.sbd.ScopeFactory;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPI;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPO;

@Component
@RequiredArgsConstructor
public class NextMoveOutMessageFactory {

    private final IntegrasjonspunktProperties properties;
    private final ServiceRecordProvider serviceRecordProvider;
    private final UUIDGenerator uuidGenerator;
    private final Clock clock;

    NextMoveOutMessage getNextMoveOutMessage(StandardBusinessDocument sbd) {
        ServiceIdentifier serviceIdentifier = serviceRecordProvider.getServiceIdentifier(sbd);

        setDefaults(sbd, serviceIdentifier);

        return new NextMoveOutMessage(
                sbd.getConversationId(),
                sbd.getMessageId(),
                sbd.getProcess(),
                sbd.getReceiverIdentifier() != null ? sbd.getReceiverIdentifier().getPrimaryIdentifier() : null,
                sbd.getSenderIdentifier().getPrimaryIdentifier(),
                serviceIdentifier,
                sbd);
    }

    private void setDefaults(StandardBusinessDocument sbd, ServiceIdentifier serviceIdentifier) {
        sbd.getScopes()
                .stream()
                .filter(p -> !StringUtils.hasText(p.getInstanceIdentifier()))
                .forEach(p -> p.setInstanceIdentifier(uuidGenerator.generate()));

        if (sbd.getSenderIdentifier() == null) {
            Iso6523 org = Iso6523.of(ICD.NO_ORG, properties.getOrg().getIdentifier());
            sbd.getStandardBusinessDocumentHeader().addSender(
                    new Partner()
                            .setIdentifier(new PartnerIdentification()
                                    .setValue(org.getIdentifier())
                                    .setAuthority(org.getAuthority()))
            );
        }

        DocumentIdentification documentIdentification = sbd.getStandardBusinessDocumentHeader().getDocumentIdentification();

        if (documentIdentification.getInstanceIdentifier() == null) {
            documentIdentification.setInstanceIdentifier(uuidGenerator.generate());
        }

        if (documentIdentification.getCreationDateAndTime() == null) {
            documentIdentification.setCreationDateAndTime(OffsetDateTime.now(clock));
        }

        if (!sbd.getExpectedResponseDateTime().isPresent()) {
            OffsetDateTime ttl = OffsetDateTime.now(clock).plusHours(properties.getNextmove().getDefaultTtlHours());

            Scope scope = sbd.getScope(ScopeType.CONVERSATION_ID)
                    .orElseThrow(() -> new NextMoveRuntimeException("Missing conversation ID scope!"));

            if (scope.getScopeInformation().isEmpty()) {
                CorrelationInformation ci = new CorrelationInformation().setExpectedResponseDateTime(ttl);
                scope.getScopeInformation().add(ci);
            } else {
                scope.getScopeInformation().stream()
                        .findFirst()
                        .ifPresent(ci -> ci.setExpectedResponseDateTime(ttl));
            }
        }

        if (serviceIdentifier == DPO && !isNullOrEmpty(properties.getDpo().getMessageChannel())) {
            Optional<Scope> mcScope = SBDUtil.getOptionalMessageChannel(sbd);
            if (!mcScope.isPresent()) {
                sbd.addScope(ScopeFactory.fromIdentifier(ScopeType.MESSAGE_CHANNEL, properties.getDpo().getMessageChannel()));
            }
            if (mcScope.isPresent() && isNullOrEmpty(mcScope.get().getIdentifier())) {
                mcScope.get().setIdentifier(properties.getDpo().getMessageChannel());
            }
        }

        if (serviceIdentifier == DPI) {
            setDpiDefaults(sbd);
        }
    }

    private void setDpiDefaults(StandardBusinessDocument sbd) {
        MessageType messageType = MessageType.valueOfType(sbd.getType())
                .filter(p -> p.getApi() == ApiType.NEXTMOVE)
                .orElseThrow(() -> new UnknownMessageTypeException(sbd.getType()));

        if (messageType == MessageType.PRINT) {
            DpiPrintMessage dpiMessage = (DpiPrintMessage) sbd.getAny();

            if (dpiMessage.getUtskriftsfarge() == null) {
                dpiMessage.setUtskriftsfarge(PrintColor.SORT_HVIT);
            }

            if (dpiMessage.getPosttype() == null) {
                dpiMessage.setPosttype(PostalCategory.B_OEKONOMI);
            }

            if (sbd.getReceiverIdentifier() != null) {
                if (dpiMessage.getMottaker() == null) {
                    dpiMessage.setMottaker(new PostAddress());
                }
                if (dpiMessage.getRetur() == null) {
                    dpiMessage.setRetur(new MailReturn()
                            .setMottaker(new PostAddress())
                            .setReturhaandtering(ReturnHandling.DIREKTE_RETUR));
                }
                ServiceRecord serviceRecord = serviceRecordProvider.getServiceRecord(sbd);
                setReceiverDefaults(dpiMessage.getMottaker(), serviceRecord.getPostAddress());
                setReceiverDefaults(dpiMessage.getRetur().getMottaker(), serviceRecord.getReturnAddress());
            }
        }
    }

    private void setReceiverDefaults(PostAddress receiver, no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress srPostAddress) {
        if (!StringUtils.hasText(receiver.getNavn())) {
            receiver.setNavn(srPostAddress.getName());
        }
        if (isNullOrEmpty(receiver.getAdresselinje1())) {
            String[] addressLines = srPostAddress.getStreet().split(";");
            for (int i = 0; i < Math.min(addressLines.length, 4); i++) {
                try {
                    PropertyUtils.setProperty(receiver, "adresselinje" + (i + 1), addressLines[i]);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new NextMoveRuntimeException(e);
                }
            }
        }
        if (!StringUtils.hasText(receiver.getPostnummer())) {
            receiver.setPostnummer(srPostAddress.getPostalCode());
        }
        if (!StringUtils.hasText(receiver.getPoststed())) {
            receiver.setPoststed(srPostAddress.getPostalArea());
        }
        if (!StringUtils.hasText(receiver.getLand())) {
            receiver.setLand(srPostAddress.getCountry());
        }
    }
}
