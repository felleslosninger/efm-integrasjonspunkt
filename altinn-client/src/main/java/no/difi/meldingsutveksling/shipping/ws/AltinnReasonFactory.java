package no.difi.meldingsutveksling.shipping.ws;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicConfirmDownloadedBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicGetAvailableFilesBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamedDownloadFileStreamedBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage;

/**
 * Creates AltinnReason to contain the SOAP faults from Altinn
 */
@UtilityClass
public class AltinnReasonFactory {

    public static AltinnReason from(IBrokerServiceExternalBasicCheckIfAvailableFilesBasicAltinnFaultFaultFaultMessage fault) {
        return new AltinnReason(fault.getFaultInfo().getErrorID(),
                fault.getFaultInfo().getAltinnErrorMessage().getValue(),
                fault.getFaultInfo().getUserId().getValue(),
                fault.getFaultInfo().getAltinnLocalizedErrorMessage().getValue());
    }

    public static AltinnReason from(IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage fault) {
        return new AltinnReason(fault.getFaultInfo().getErrorID(),
                fault.getFaultInfo().getAltinnErrorMessage().getValue(),
                fault.getFaultInfo().getUserId().getValue(),
                fault.getFaultInfo().getAltinnLocalizedErrorMessage().getValue());
    }

    public static AltinnReason from(IBrokerServiceExternalBasicGetAvailableFilesBasicAltinnFaultFaultFaultMessage availableFilesFault) {
        return new AltinnReason(availableFilesFault.getFaultInfo().getErrorID(),
                availableFilesFault.getFaultInfo().getAltinnErrorMessage().getValue(),
                availableFilesFault.getFaultInfo().getUserId().getValue(),
                availableFilesFault.getFaultInfo().getAltinnLocalizedErrorMessage().getValue());
    }

    public static AltinnReason from(IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage uploadFault) {
        return new AltinnReason(uploadFault.getFaultInfo().getErrorID(),
                uploadFault.getFaultInfo().getAltinnErrorMessage().getValue(),
                uploadFault.getFaultInfo().getUserId().getValue(),
                uploadFault.getFaultInfo().getAltinnLocalizedErrorMessage().getValue());
    }

    public static AltinnReason from(IBrokerServiceExternalBasicStreamedDownloadFileStreamedBasicAltinnFaultFaultFaultMessage downloadFault) {
        return new AltinnReason(downloadFault.getFaultInfo().getErrorID(),
                downloadFault.getFaultInfo().getAltinnErrorMessage().getValue(),
                downloadFault.getFaultInfo().getUserId().getValue(),
                downloadFault.getFaultInfo().getAltinnLocalizedErrorMessage().getValue());
    }

    public static AltinnReason from(IBrokerServiceExternalBasicConfirmDownloadedBasicAltinnFaultFaultFaultMessage confirmFault) {
        return new AltinnReason(confirmFault.getFaultInfo().getErrorID(),
                confirmFault.getFaultInfo().getAltinnErrorMessage().getValue(),
                confirmFault.getFaultInfo().getUserId().getValue(),
                confirmFault.getFaultInfo().getAltinnLocalizedErrorMessage().getValue());
    }

}
