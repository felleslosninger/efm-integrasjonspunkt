package no.difi.meldingsutveksling.validation

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer
import org.springframework.boot.diagnostics.FailureAnalysis
import java.security.cert.CertificateExpiredException

class CertificateExpirationAnalyzer : AbstractFailureAnalyzer<CertificateExpiredException>() {

    override fun analyze(rootFailure: Throwable, cause: CertificateExpiredException): FailureAnalysis {
        return FailureAnalysis("Certificate not valid: ${cause.message}", "Update to a valid certificate", cause)
    }

}

class VirksertCertificateAnalyzer : AbstractFailureAnalyzer<VirksertCertificateException>() {

    override fun analyze(rootFailure: Throwable, cause: VirksertCertificateException): FailureAnalysis {
        return FailureAnalysis("Certificate expired or not found in virksert: ${cause.message}", "Contact servicedesk@digdir.no and supply a valid certificate", cause)
    }

}