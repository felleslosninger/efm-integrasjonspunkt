package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.DpvConversationResource;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;

import java.time.LocalDateTime;
import java.util.Optional;


public class PostVirksomhetStrategyFactory implements MessageStrategyFactory {

    private final CorrespondenceAgencyConfiguration configuration;
    private final NoarkClient noarkClient;

    private PostVirksomhetStrategyFactory(CorrespondenceAgencyConfiguration configuration, NoarkClient noarkClient) {
        this.configuration = configuration;
        this.noarkClient = noarkClient;
    }

    public static PostVirksomhetStrategyFactory newInstance(IntegrasjonspunktProperties properties,
                                                            NoarkClient noarkClient,
                                                            ServiceRegistryLookup serviceRegistryLookup) {

        InfoRecord infoRecord = serviceRegistryLookup.getInfoRecord(properties.getOrg().getNumber());
        CorrespondenceAgencyConfiguration.CorrespondenceAgencyConfigurationBuilder builder = CorrespondenceAgencyConfiguration.builder()
                .externalServiceCode(properties.getDpv().getExternalServiceCode())
                .externalServiceEditionCode(properties.getDpv().getExternalServiceEditionCode())
                .password(properties.getDpv().getPassword())
                .systemUserCode(properties.getDpv().getUsername())
                .sender(infoRecord.getOrganizationName())
                .notifyEmail(properties.getDpv().isNotifyEmail())
                .notifySms(properties.getDpv().isNotifySms());

        Optional<String> notificationText = Optional.ofNullable(properties.getDpv())
                .map(IntegrasjonspunktProperties.PostVirksomheter::getNotificationText);
        notificationText.ifPresent(builder::notificationText);

        builder.nextmoveFiledir(properties.getNextmove().getFiledir());

        builder.endpointUrl(properties.getDpv().getEndpointUrl().toString());

        CorrespondenceAgencyConfiguration config = builder.build();

        return new PostVirksomhetStrategyFactory(config, noarkClient);
    }

    public static PostVirksomhetStrategyFactory newInstance(IntegrasjonspunktProperties properties,
                                                            DpvConversationResource cr) {

        CorrespondenceAgencyConfiguration.CorrespondenceAgencyConfigurationBuilder builder = CorrespondenceAgencyConfiguration.builder()
                .externalServiceCode(properties.getDpv().getExternalServiceCode())
                .externalServiceEditionCode(cr.getServiceEdition())
                .password(properties.getDpv().getPassword())
                .systemUserCode(properties.getDpv().getUsername())
                .sender(cr.getReceiver().getReceiverName())
                .languageCode(cr.getLanguageCode())
                .allowForwarding(cr.isAllowForwarding())
                .notifyEmail(properties.getDpv().isNotifyEmail())
                .notifySms(properties.getDpv().isNotifySms());

        if (cr.getVisibleDateTime() != null) {
            builder.visibleDateTime(cr.getVisibleDateTime());
        } else {
            builder.visibleDateTime(LocalDateTime.now());
        }

        if (cr.getAllowSystemDeleteDateTime() != null) {
            builder.allowSystemDeleteTime(cr.getAllowSystemDeleteDateTime());
        } else {
            builder.allowSystemDeleteTime(LocalDateTime.now().plusMinutes(5));
        }

        if (cr.getNotifications() != null) {
            builder.emailText(cr.getNotifications().getEmailNotification().getText());
            builder.smsText(cr.getNotifications().getSmsNotification().getText());
        }

        builder.nextmoveFiledir(properties.getNextmove().getFiledir());

        builder.endpointUrl(properties.getDpv().getEndpointUrl().toString());

        CorrespondenceAgencyConfiguration config = builder.build();

        return new PostVirksomhetStrategyFactory(config, null);
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new PostVirksomhetMessageStrategy(configuration, noarkClient);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPV;
    }

    public CorrespondenceAgencyConfiguration getConfig() {
        return this.configuration;
    }
}
