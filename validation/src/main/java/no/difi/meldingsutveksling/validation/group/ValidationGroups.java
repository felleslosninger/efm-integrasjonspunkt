package no.difi.meldingsutveksling.validation.group;

import javax.validation.groups.Default;

public interface ValidationGroups {

    interface DocumentType extends Default {

        interface Arkivmelding extends DocumentType {
        }

        interface ArkivmeldingKvittering extends DocumentType {
        }

        interface Avtalt extends DocumentType {
        }

        interface Digital extends DocumentType {
        }

        interface DigitalDpv extends DocumentType {
        }

        interface Print extends DocumentType {
        }

        interface Innsynskrav extends DocumentType {
        }

        interface Publisering extends DocumentType {
        }

        interface EInnsynKvittering extends DocumentType {
        }
    }

    interface ServiceIdentifier extends Default {

        interface DPE extends ServiceIdentifier {
        }

        interface DPF extends ServiceIdentifier {
        }

        interface DPI extends ServiceIdentifier {
        }

        interface DPO extends ServiceIdentifier {
        }

        interface DPV extends ServiceIdentifier {
        }
    }

    interface Partner extends Default {

        interface Sender extends Partner {
        }

        interface Receiver extends Partner {
        }
    }

    interface Create extends Default {

    }

    interface Update extends Default {

    }
}