package no.difi.meldingsutveksling.validation.group;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.DocumentType;

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
}
