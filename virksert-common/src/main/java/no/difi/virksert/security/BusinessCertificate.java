package no.difi.virksert.security;

import no.difi.certvalidator.Validator;
import no.difi.certvalidator.ValidatorBuilder;
import no.difi.certvalidator.api.CertificateBucket;
import no.difi.certvalidator.api.CrlCache;
import no.difi.certvalidator.api.PrincipalNameProvider;
import no.difi.certvalidator.extra.NorwegianOrganizationNumberRule;
import no.difi.certvalidator.rule.*;
import no.difi.certvalidator.structure.Junction;
import no.difi.certvalidator.util.KeyStoreCertificateBucket;
import no.difi.certvalidator.util.SimpleCrlCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessCertificate {

    private static final Logger logger = LoggerFactory.getLogger(BusinessCertificate.class);

    private static CrlCache crlCache = new SimpleCrlCache();
    private static String[] acceptedPolicies = {
            "2.16.578.1.26.1.3.5",
            "2.16.578.1.29.13.1.1.0",
            "2.16.578.1.26.1.3.2",
            "2.16.578.1.26.1.0.3.2",
            "2.16.578.1.29.913.1.1.0",
            "2.16.578.1.1.1.1.100"
    };

    public static Validator getValidator(String scope, String[] rootAliases, String[] intermediatAliases) {
        try {
            KeyStoreCertificateBucket keyStoreCertificateBucket = new KeyStoreCertificateBucket(BusinessCertificate.class.getResourceAsStream("/" + scope + ".jks"), "changeit");
            CertificateBucket rootCertificates = keyStoreCertificateBucket.toSimple(rootAliases);
            CertificateBucket intermediateCertificates = keyStoreCertificateBucket.toSimple(intermediatAliases);

            return ValidatorBuilder.newInstance()
                    .addRule(new ExpirationRule())
                    .addRule(SigningRule.PublicSignedOnly())
                    .addRule(Junction.or()
                                    .addRule(new CriticalOidRule("2.5.29.15"))
                                    .addRule(new CriticalOidRule("2.5.29.15", "2.5.29.19"))
                    )
                    .addRule(new ChainRule(rootCertificates, intermediateCertificates, acceptedPolicies))
                    .addRule(new NorwegianOrganizationNumberRule(new PrincipalNameProvider() {
                        @Override
                        public boolean validate(String s) {
                            // Accept all organization numbers.
                            logger.debug(s);
                            return true;
                        }
                    }))
                    .addRule(new CRLRule(crlCache))
                    //.addRule(new OCSPRule(intermediateCertificates))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
