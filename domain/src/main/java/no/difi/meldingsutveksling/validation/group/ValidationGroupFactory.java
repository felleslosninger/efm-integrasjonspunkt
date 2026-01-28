package no.difi.meldingsutveksling.validation.group;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.ServiceIdentifier;

@UtilityClass
public class ValidationGroupFactory {

    public static Class<?> toDocumentType(no.difi.meldingsutveksling.MessageType in) {
        switch (in) {
            case FIKSIO:
                return NextMoveValidationGroups.MessageType.FiksIo.class;
            case ARKIVMELDING:
                return NextMoveValidationGroups.MessageType.Arkivmelding.class;
            case ARKIVMELDING_KVITTERING:
                return NextMoveValidationGroups.MessageType.ArkivmeldingKvittering.class;
            case AVTALT:
                return NextMoveValidationGroups.MessageType.Avtalt.class;
            case DIALOGMELDING:
                return NextMoveValidationGroups.MessageType.Dialogmelding.class;
            case PRINT:
                return NextMoveValidationGroups.MessageType.Print.class;
            case DIGITAL:
                return NextMoveValidationGroups.MessageType.Digital.class;
            case DIGITAL_DPV:
                return NextMoveValidationGroups.MessageType.DigitalDpv.class;
            case INNSYNSKRAV:
                return NextMoveValidationGroups.MessageType.Innsynskrav.class;
            case PUBLISERING:
                return NextMoveValidationGroups.MessageType.Publisering.class;
            case EINNSYN_KVITTERING:
                return NextMoveValidationGroups.MessageType.EInnsynKvittering.class;
            default:
                return null;
        }
    }

    public static Class<?> toServiceIdentifier(ServiceIdentifier serviceIdentifier) {
        if (serviceIdentifier == null) throw new IllegalArgumentException("ServiceIdentifier cannot be null");
        switch (serviceIdentifier) {
            case DPE:
                return NextMoveValidationGroups.ServiceIdentifier.DPE.class;
            case DPF:
                return NextMoveValidationGroups.ServiceIdentifier.DPF.class;
            case DPI:
                return NextMoveValidationGroups.ServiceIdentifier.DPI.class;
            case DPO:
                return NextMoveValidationGroups.ServiceIdentifier.DPO.class;
            case DPH:
                return NextMoveValidationGroups.ServiceIdentifier.DPH.class;
            case DPV:
                return NextMoveValidationGroups.ServiceIdentifier.DPV.class;
            case DPFIO:
                return NextMoveValidationGroups.ServiceIdentifier.DPFIO.class;
            case UNKNOWN:
                return NextMoveValidationGroups.ServiceIdentifier.UNKNOWN.class;
            default:
                throw new IllegalArgumentException("Missing case for ServiceIdentifier: "+serviceIdentifier);
        }
    }

}
