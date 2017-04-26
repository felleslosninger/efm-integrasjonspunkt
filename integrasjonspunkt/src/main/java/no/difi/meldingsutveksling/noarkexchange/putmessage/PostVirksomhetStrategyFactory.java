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
                .withExternalServiceCode(properties.getAltinnPTV().getExternalServiceCode())
                .withExternalServiceEditionCode(properties.getAltinnPTV().getExternalServiceEditionCode())
                .withPassword(properties.getAltinnPTV().getPassword())
                .withSystemUserCode(properties.getAltinnPTV().getUsername())
                .withSender(infoRecord.getOrganizationName());

        Optional<String> smsVarslingstekst = Optional.of(properties.getAltinnPTV()).map(IntegrasjonspunktProperties
                .PostVirksomheter::getSms).map(IntegrasjonspunktProperties.Sms::getVarslingstekst);
        if (smsVarslingstekst.isPresent() && !"".equals(smsVarslingstekst.get())) {
            builder.withSmsText(smsVarslingstekst.get());
        }

        Optional<String> emailSubject = Optional.of(properties.getAltinnPTV()).map(IntegrasjonspunktProperties
                .PostVirksomheter::getEmail).map(IntegrasjonspunktProperties.PostVirksomheter.Email::getEmne);
        if (emailSubject.isPresent() && !"".equals(emailSubject.get())) {
            builder.withEmailSubject(emailSubject.get()).withEmailBody(properties.getAltinnPTV().getEmail().getVarslingstekst());
        }
        CorrespondenceAgencyConfiguration config = builder.build();
        return new PostVirksomhetStrategyFactory(config, serviceRegistryLookup);
    }

    @Override
    public MessageStrategy create(Object payload) {
        return new PostVirksomhetMessageStrategy(configuration, serviceRegistryLookup);
    }

    @Override
    public ServiceIdentifier getServiceIdentifier() {
        return ServiceIdentifier.DPV;
    }
}
