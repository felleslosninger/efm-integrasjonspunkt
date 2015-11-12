package no.difi.meldingsutveksling.shipping.ws;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicGetAvailableFilesBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamedDownloadFileStreamedBasicAltinnFaultFaultFaultMessage;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage;

/**
 * Creates AltinnReason to contain the SOAP faults from Altinn
 */
public class AltinnReasonFactory {
    public static AltinnReason from(IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage initateAltinnFault) {
        String message = initateAltinnFault.getFaultInfo().getAltinnErrorMessage().getValue();
        Integer id = initateAltinnFault.getFaultInfo().getErrorID();
        String userId = initateAltinnFault.getFaultInfo().getUserId().getValue();
        return new AltinnReason(id, message, userId);
    }

    public static AltinnReason from(IBrokerServiceExternalBasicGetAvailableFilesBasicAltinnFaultFaultFaultMessage availableFilesFault) {
        String message = availableFilesFault.getFaultInfo().getAltinnErrorMessage().getValue();
        Integer id = availableFilesFault.getFaultInfo().getErrorID();
        String userId = availableFilesFault.getFaultInfo().getUserId().getValue();
        return new AltinnReason(id, message, userId);
    }

    public static AltinnReason from(IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage uploadFault) {
        String message = uploadFault.getFaultInfo().getAltinnErrorMessage().getValue();
        Integer id = uploadFault.getFaultInfo().getErrorID();
        String userId = uploadFault.getFaultInfo().getUserId().getValue();
        return new AltinnReason(id, message, userId);
    }

    public static AltinnReason from(IBrokerServiceExternalBasicStreamedDownloadFileStreamedBasicAltinnFaultFaultFaultMessage downloadFault) {
        String message = downloadFault.getFaultInfo().getAltinnErrorMessage().getValue();
        Integer id = downloadFault.getFaultInfo().getErrorID();
        String userId = downloadFault.getFaultInfo().getUserId().getValue();
        return new AltinnReason(id, message, userId);
    }
}
