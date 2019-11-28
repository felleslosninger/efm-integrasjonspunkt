package no.difi.meldingsutveksling.validation.group;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.ServiceIdentifier;

@UtilityClass
public class ValidationGroupFactory {

    public static Class<?> toDocumentType(DocumentType in) {
        switch (in) {
            case ARKIVMELDING:
                return ValidationGroups.DocumentType.Arkivmelding.class;
            case ARKIVMELDING_KVITTERING:
                return ValidationGroups.DocumentType.ArkivmeldingKvittering.class;
            case PRINT:
                return ValidationGroups.DocumentType.Print.class;
            case DIGITAL:
                return ValidationGroups.DocumentType.Digital.class;
            case DIGITAL_DPV:
                return ValidationGroups.DocumentType.DigitalDpv.class;
            case INNSYNSKRAV:
                return ValidationGroups.DocumentType.Innsynskrav.class;
            case PUBLISERING:
                return ValidationGroups.DocumentType.Publisering.class;
            case EINNSYN_KVITTERING:
                return ValidationGroups.DocumentType.EInnsynKvittering.class;
            default:
                return null;
        }
    }

    public static Class<?> toServiceIdentifier(ServiceIdentifier serviceIdentifier) {
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
            default:
                return null;
        }
    }

}
