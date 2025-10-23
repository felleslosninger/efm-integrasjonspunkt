package no.difi.meldingsutveksling.validation;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

import java.security.cert.CertificateExpiredException;

public class CertificateExpirationAnalyzer extends AbstractFailureAnalyzer<CertificateExpiredException> {
    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, CertificateExpiredException cause) {
        return new FailureAnalysis("Certificate not valid: " + cause.getMessage(), "Update to a valid certificate", cause);
        }
}
