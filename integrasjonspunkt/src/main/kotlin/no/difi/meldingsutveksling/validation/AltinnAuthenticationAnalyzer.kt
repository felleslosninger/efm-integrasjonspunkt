package no.difi.meldingsutveksling.validation

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage
import no.difi.meldingsutveksling.shipping.ws.AltinnReasonFactory
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer
import org.springframework.boot.diagnostics.FailureAnalysis

class AltinnAuthenticationAnalyzer :
    AbstractFailureAnalyzer<IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage>() {

    override fun analyze(
        rootFailure: Throwable,
        cause: IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage
    ): FailureAnalysis {
        val altinnReason = AltinnReasonFactory.from(cause)
        val errorMsg = "Failed to connect to Altinn - $altinnReason"
        if (altinnReason.message.contains("Incorrect username/password")) {
            return FailureAnalysis(errorMsg, "Update username and/or password", cause)
        }
        return FailureAnalysis(errorMsg, "Verify Altinn connection details", cause)
    }

}