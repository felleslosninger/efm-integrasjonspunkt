package no.difi.meldingsutveksling.validation.group;

import no.difi.meldingsutveksling.ServiceIdentifier;

import javax.validation.groups.Default;

public interface ValidationGroups {

    interface ServiceIdentifier extends Default {

        interface Dpo extends ServiceIdentifier {
        }

        interface Dpv extends ServiceIdentifier {
        }

        interface DpiDigital extends ServiceIdentifier {
        }

        interface DpiPrint extends ServiceIdentifier {
        }

        interface Dpf extends ServiceIdentifier {
        }

        interface DpeInnsyn extends ServiceIdentifier {
        }

        interface DpeData extends ServiceIdentifier {
        }

        interface DpeReceipt extends ServiceIdentifier {
        }
    }
}
