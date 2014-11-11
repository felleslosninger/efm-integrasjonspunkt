package no.difi.messagehandler.peppolmessageutils;

/**
 * @author Kubilay Karayilan
 *         Kubilay.Karayilan@inmeta.no
 *         created on 10.11.2014.
 */
public enum ProcessStates {
    WRONG_TYPE(0),
    DECRYPTION_ERROR(1),
    SIGNATURE_VALIDATION_ERROR(1),
    NO_ARKIVE_UNAVAILABLE(3),
    SBD_PACKAGING_FEIL(4);
    private int value;
    private ProcessStates(int value){
        this.value = value;
    }
}
