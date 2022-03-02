package no.difi.meldingsutveksling.validation.group;

import javax.validation.groups.Default;

public interface NextMoveValidationGroups {

    interface MessageType extends Default {

        interface FiksIo extends MessageType {
        }

        interface Arkivmelding extends MessageType {
        }

        interface ArkivmeldingKvittering extends MessageType {
        }

        interface Avtalt extends MessageType {
        }

        interface Digital extends MessageType {
        }

        interface DigitalDpv extends MessageType {
        }

        interface Print extends MessageType {
        }

        interface Innsynskrav extends MessageType {
        }

        interface Publisering extends MessageType {
        }

        interface EInnsynKvittering extends MessageType {
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

        interface DPFIO extends ServiceIdentifier {
        }

        interface UNKNOWN extends ServiceIdentifier {
        }
    }
}