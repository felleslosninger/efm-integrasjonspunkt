package no.difi.meldingsutveksling.validation.group;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.ServiceIdentifier;

@UtilityClass
public class ValidationGroupFactory {

    public static Class<?> toDocumentType(no.difi.meldingsutveksling.MessageType in) {
        switch (in) {
            case FIKSIO:
                return ValidationGroups.MessageType.FiksIo.class;
            case ARKIVMELDING:
                return ValidationGroups.MessageType.Arkivmelding.class;
            case ARKIVMELDING_KVITTERING:
                return ValidationGroups.MessageType.ArkivmeldingKvittering.class;
            case AVTALT:
                return ValidationGroups.MessageType.Avtalt.class;
            case PRINT:
                return ValidationGroups.MessageType.Print.class;
            case DIGITAL:
                return ValidationGroups.MessageType.Digital.class;
            case DIGITAL_DPV:
                return ValidationGroups.MessageType.DigitalDpv.class;
            case INNSYNSKRAV:
                return ValidationGroups.MessageType.Innsynskrav.class;
            case PUBLISERING:
                return ValidationGroups.MessageType.Publisering.class;
            case EINNSYN_KVITTERING:
                return ValidationGroups.MessageType.EInnsynKvittering.class;
            default:
                return null;
        }
    }

    public static Class<?> toServiceIdentifier(ServiceIdentifier serviceIdentifier) {
        if (serviceIdentifier == null) throw new IllegalArgumentException("ServiceIdentifier cannot be null");
        switch (serviceIdentifier) {
            case DPE:
                return ValidationGroups.ServiceIdentifier.DPE.class;
            case DPF:
                return ValidationGroups.ServiceIdentifier.DPF.class;
            case DPI:
                return ValidationGroups.ServiceIdentifier.DPI.class;
            case DPO:
                return ValidationGroups.ServiceIdentifier.DPO.class;
            case DPV:
                return ValidationGroups.ServiceIdentifier.DPV.class;
            case DPFIO:
                return ValidationGroups.ServiceIdentifier.DPFIO.class;
            case UNKNOWN:
                return ValidationGroups.ServiceIdentifier.UNKNOWN.class;
            default:
                throw new IllegalArgumentException("Missing case for ServiceIdentifier: "+serviceIdentifier);
        }
    }

}
