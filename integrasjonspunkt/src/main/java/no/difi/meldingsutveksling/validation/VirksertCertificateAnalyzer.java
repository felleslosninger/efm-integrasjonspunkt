package no.difi.meldingsutveksling.validation;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class VirksertCertificateAnalyzer extends AbstractFailureAnalyzer<VirksertCertificateException> {
    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, VirksertCertificateException cause) {
        return new FailureAnalysis(
                "Virksert certificate validation failed: " + cause.getMessage(),
                "Contact servicedesk@digdir.no and supply a valid certificate",
                cause
        );
    }
}
