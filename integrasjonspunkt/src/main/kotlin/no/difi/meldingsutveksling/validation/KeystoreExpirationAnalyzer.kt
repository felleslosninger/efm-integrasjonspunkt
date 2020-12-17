package no.difi.meldingsutveksling.validation

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer
import org.springframework.boot.diagnostics.FailureAnalysis
import java.security.cert.CertificateExpiredException

class KeystoreExpirationAnalyzer : AbstractFailureAnalyzer<CertificateExpiredException>() {

    override fun analyze(rootFailure: Throwable, cause: CertificateExpiredException): FailureAnalysis {
        return FailureAnalysis("Certificate not valid: ${cause.message}", "Update to a valid certificate", cause)
    }
    
}