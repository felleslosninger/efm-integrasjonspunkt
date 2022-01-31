package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.MessageType;
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
        ServiceRecord serviceRecord = serviceRecordProvider.getServiceRecord(sbd);

        setDefaults(sbd, serviceRecord);

        return new NextMoveOutMessage(
                SBDUtil.getConversationId(sbd),
                SBDUtil.getMessageId(sbd),
                SBDUtil.getProcess(sbd),
                sbd.getReceiverIdentifier().getPrimaryIdentifier(),
                sbd.getSenderIdentifier().getPrimaryIdentifier(),
                serviceRecord.getServiceIdentifier(),
                sbd);
    }

    private void setDefaults(StandardBusinessDocument sbd, ServiceRecord serviceRecord) {
        sbd.getScopes()
                .stream()
                .filter(p -> !StringUtils.hasText(p.getInstanceIdentifier()))
                .forEach(p -> p.setInstanceIdentifier(uuidGenerator.generate()));

        if (sbd.getSenderIdentifier() == null) {
            Iso6523 org = Iso6523.of(ICD.NO_ORG, properties.getOrg().getNumber());
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

        if (serviceRecord.getServiceIdentifier() == DPO && !isNullOrEmpty(properties.getDpo().getMessageChannel())) {
            Optional<Scope> mcScope = SBDUtil.getOptionalMessageChannel(sbd);
            if (!mcScope.isPresent()) {
                sbd.addScope(ScopeFactory.fromIdentifier(ScopeType.MESSAGE_CHANNEL, properties.getDpo().getMessageChannel()));
            }
            if (mcScope.isPresent() && isNullOrEmpty(mcScope.get().getIdentifier())) {
                mcScope.get().setIdentifier(properties.getDpo().getMessageChannel());
            }
        }

        if (serviceRecord.getServiceIdentifier() == DPI) {
            setDpiDefaults(sbd, serviceRecord);
        }
    }

    private void setDpiDefaults(StandardBusinessDocument sbd, ServiceRecord serviceRecord) {
        MessageType messageType = SBDUtil.getOptionalMessageType(sbd)
                .filter(p -> p.getApi() == ApiType.NEXTMOVE)
                .orElseThrow(() -> new UnknownMessageTypeException(sbd.getType().orElse("null")));

        if (messageType == MessageType.PRINT) {
            DpiPrintMessage dpiMessage = (DpiPrintMessage) sbd.getAny();
            if (dpiMessage.getMottaker() == null) {
                dpiMessage.setMottaker(new PostAddress());
            }
            setReceiverDefaults(dpiMessage.getMottaker(), serviceRecord.getPostAddress());
            if (dpiMessage.getRetur() == null) {
                dpiMessage.setRetur(new MailReturn()
                        .setMottaker(new PostAddress())
                        .setReturhaandtering(ReturnHandling.DIREKTE_RETUR));
            }
            setReceiverDefaults(dpiMessage.getRetur().getMottaker(), serviceRecord.getReturnAddress());

            if (dpiMessage.getUtskriftsfarge() == null) {
                dpiMessage.setUtskriftsfarge(PrintColor.SORT_HVIT);
            }

            if (dpiMessage.getPosttype() == null) {
                dpiMessage.setPosttype(PostalCategory.B_OEKONOMI);
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
