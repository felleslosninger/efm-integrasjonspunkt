package no.difi.meldingsutveksling.validation.group;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ValidationGroupFactory {

    public static Class<?> toServiceIdentifier(no.difi.meldingsutveksling.ServiceIdentifier in) {
        switch (in) {
            case DPO:
                return ValidationGroups.ServiceIdentifier.Dpo.class;
            case DPV:
                return ValidationGroups.ServiceIdentifier.Dpv.class;
            case DPI_DIGITAL:
                return ValidationGroups.ServiceIdentifier.DpiDigital.class;
            case DPI_PRINT:
                return ValidationGroups.ServiceIdentifier.DpiPrint.class;
            case DPF:
                return ValidationGroups.ServiceIdentifier.Dpf.class;
            case DPE_INNSYN:
                return ValidationGroups.ServiceIdentifier.DpeInnsyn.class;
            case DPE_DATA:
                return ValidationGroups.ServiceIdentifier.DpeData.class;
            case DPE_RECEIPT:
                return ValidationGroups.ServiceIdentifier.DpeReceipt.class;
            default:
                return null;
        }
    }
}
