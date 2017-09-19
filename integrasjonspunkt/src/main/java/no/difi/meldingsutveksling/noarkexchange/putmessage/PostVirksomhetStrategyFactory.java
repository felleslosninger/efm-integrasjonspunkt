package no.difi.meldingsutveksling.noarkexchange.putmessage;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ptv.CorrespondenceAgencyConfiguration;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;

import java.util.Optional;


public class PostVirksomhetStrategyFactory implements MessageStrategyFactory {

    private final CorrespondenceAgencyConfiguration configuration;
    private final ServiceRegistryLookup serviceRegistryLookup;

    private PostVirksomhetStrategyFactory(CorrespondenceAgencyConfiguration configuration, ServiceRegistryLookup serviceRegistryLookup) {
        this.configuration = configuration;
        this.serviceRegistryLookup = serviceRegistryLookup;
    }

    public static PostVirksomhetStrategyFactory newInstance(IntegrasjonspunktProperties properties,
                                                            ServiceRegistryLookup serviceRegistryLookup) {

        InfoRecord infoRecord = serviceRegistryLookup.getInfoRecord(properties.getOrg().getNumber());
        CorrespondenceAgencyConfiguration.Builder builder = new CorrespondenceAgencyConfiguration.Builder()
                .withExternalServiceCode(properties.getDpv().getExternalServiceCode())
                .withExternalServiceEditionCode(properties.getDpv().getExternalServiceEditionCode())
                .withPassword(properties.getDpv().getPassword())
                .withSystemUserCode(properties.getDpv().getUsername())
                .withSender(infoRecord.getOrganizationName());

        Optional<String> smsVarslingstekst = Optional.of(properties.getDpv()).map(IntegrasjonspunktProperties
                .PostVirksomheter::getSms).map(IntegrasjonspunktProperties.Sms::getVarslingstekst);
        if (smsVarslingstekst.isPresent() && !"".equals(smsVarslingstekst.get())) {
            builder.withSmsText(smsVarslingstekst.get());
        }

        Optional<String> emailSubject = Optional.of(properties.getDpv()).map(IntegrasjonspunktProperties
                .PostVirksomheter::getEmail).map(IntegrasjonspunktProperties.PostVirksomheter.Email::getEmne);
        if (emailSubject.isPresent() && !"".equals(emailSubject.get())) {
            builder.withEmailSubject(emailSubject.get()).withEmailBody(properties.getDpv().getEmail().getVarslingstekst());
        }

        builder.withNextbestFiledir(properties.getNextbest().getFiledir());

        builder.withEndpointUrl(properties.getDpv().getEndpointUrl().toString());

        CorrespondenceAgencyConfiguration config = builder.build();
        return new PostVirksomhetStrategyFactory(config, serviceRegistryLookup);
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new PostVirksomhetMessageStrategy(configuration);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPV;
    }

    public CorrespondenceAgencyConfiguration getConfig() {
        return this.configuration;
    }
}
