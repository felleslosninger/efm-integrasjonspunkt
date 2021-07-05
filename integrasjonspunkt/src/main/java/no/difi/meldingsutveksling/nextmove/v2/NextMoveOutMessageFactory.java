package no.difi.meldingsutveksling.nextmove.v2;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.exceptions.UnknownMessageTypeException;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.sdp.client2.domain.fysisk_post.Posttype;
import no.difi.sdp.client2.domain.fysisk_post.Returhaandtering;
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.time.OffsetDateTime;

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
                SBDUtil.getReceiverIdentifier(sbd),
                SBDUtil.getSenderIdentifier(sbd),
                serviceRecord.getServiceIdentifier(),
                sbd);
    }

    private void setDefaults(StandardBusinessDocument sbd, ServiceRecord serviceRecord) {
        StandardBusinessDocumentUtils.getScopes(sbd)
                .stream()
                .filter(p -> !StringUtils.hasText(p.getInstanceIdentifier()))
                .forEach(p -> p.setInstanceIdentifier(uuidGenerator.generate()));

        if (SBDUtil.getSenderIdentifier(sbd) == null) {
            Organisasjonsnummer org = Organisasjonsnummer.from(properties.getOrg().getNumber());
            sbd.getStandardBusinessDocumentHeader().addSender(
                    new Sender()
                            .setIdentifier(new PartnerIdentification()
                                    .setValue(org.asIso6523())
                                    .setAuthority(org.authority()))
            );
        }

        DocumentIdentification documentIdentification = sbd.getStandardBusinessDocumentHeader().getDocumentIdentification();

        if (documentIdentification.getInstanceIdentifier() == null) {
            documentIdentification.setInstanceIdentifier(uuidGenerator.generate());
        }

        if (documentIdentification.getCreationDateAndTime() == null) {
            documentIdentification.setCreationDateAndTime(OffsetDateTime.now(clock));
        }

        if (!StandardBusinessDocumentUtils.getExpectedResponseDateTime(sbd).isPresent()) {
            OffsetDateTime ttl = OffsetDateTime.now(clock).plusHours(properties.getNextmove().getDefaultTtlHours());

            Scope scope = StandardBusinessDocumentUtils.getScope(sbd, ScopeType.CONVERSATION_ID)
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

        if (serviceRecord.getServiceIdentifier() == ServiceIdentifier.DPI) {
            setDpiDefaults(sbd, serviceRecord);
        }
    }

    private void setDpiDefaults(StandardBusinessDocument sbd, ServiceRecord serviceRecord) {
        MessageType messageType = SBDUtil.getOptionalMessageType(sbd)
                .filter(p -> p.getApi() == ApiType.NEXTMOVE)
                .orElseThrow(() -> new UnknownMessageTypeException(StandardBusinessDocumentUtils.getType(sbd).orElse("null")));

        if (messageType == MessageType.PRINT) {
            DpiPrintMessage dpiMessage = (DpiPrintMessage) sbd.getAny();
            if (dpiMessage.getMottaker() == null) {
                dpiMessage.setMottaker(new PostAddress());
            }
            setReceiverDefaults(dpiMessage.getMottaker(), serviceRecord.getPostAddress());
            if (dpiMessage.getRetur() == null) {
                dpiMessage.setRetur(new MailReturn()
                        .setMottaker(new PostAddress())
                        .setReturhaandtering(Returhaandtering.DIREKTE_RETUR));
            }
            setReceiverDefaults(dpiMessage.getRetur().getMottaker(), serviceRecord.getReturnAddress());

            if (dpiMessage.getUtskriftsfarge() == null) {
                dpiMessage.setUtskriftsfarge(Utskriftsfarge.SORT_HVIT);
            }

            if (dpiMessage.getPosttype() == null) {
                dpiMessage.setPosttype(Posttype.B_OEKONOMI);
            }

        }
    }

    private void setReceiverDefaults(PostAddress receiver, no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress srPostAddress) {
        if (!StringUtils.hasText(receiver.getNavn())) {
            receiver.setNavn(srPostAddress.getName());
        }
        if (Strings.isNullOrEmpty(receiver.getAdresselinje1())) {
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
