package no.difi.virksert.server.logic;

import no.difi.certvalidator.api.CertificateValidationException;
import no.difi.certvalidator.extra.NorwegianOrganizationNumberRule;
import no.difi.certvalidator.extra.NorwegianOrganizationNumberRule.NorwegianOrganization;
import no.difi.virksert.server.model.Registration;
import no.difi.virksert.server.repository.RegistrationRepository;
import no.difi.xsd.virksert.model._1.Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Service
public class RegistrationService {

    private static Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    @Autowired
    private RegistrationRepository registrationRepository;

    public Registration findByIdentifier(String identifier) {
        return registrationRepository.findByIdentifier(identifier);
    }

    public Registration save(X509Certificate certificate) {
        try {
            final NorwegianOrganization norwegianOrganization = NorwegianOrganizationNumberRule.extractNumber(certificate);
            if (norwegianOrganization == null) {
                throw new CertificateValidationException("no Norwegian OrganisationNumber in X509 certificate ");
            }
            String identifier = norwegianOrganization.getNumber();
            Registration registration = registrationRepository.findByIdentifier(identifier);
            if (registration == null)
                registration = new Registration();
            registration.update(certificate);

            registrationRepository.save(registration);
            logger.debug("Certificate saved: {} => {}", registration.getId(), registration.getIdentifier());

            return registration;
        } catch (CertificateValidationException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public List<Certificate> listRevokedAsCertificates() {
        return convertToCertificate(registrationRepository.listRevoked());
    }

    public List<Certificate> listUpdatedAsCertificates() {
        return convertToCertificate(registrationRepository.listUpdated());
    }

    public List<Certificate> listExpiredAsCertificates() {
        return convertToCertificate(registrationRepository.listRevoked());
    }

    private List<Certificate> convertToCertificate(List<Registration> registrations) {
        List<Certificate> certificates = new ArrayList<>();
        for (Registration registration : registrations)
            certificates.add(registration.toCertificate());

        return certificates;

    }
}
