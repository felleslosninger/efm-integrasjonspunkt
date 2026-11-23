package no.difi.meldingsutveksling.dph.client;

import lombok.Getter;
import no.ks.fiks.hdir.FeilmeldingForApplikasjonskvittering;

@Getter
public class DphException extends RuntimeException {

    /**
     * The adapter's technical code for an incoming message that is invalid, which is rejected with an
     * ApplicationReceipt.
     */
    public static final String INVALID_MESSAGE = "E7782";

    /**
     * The adapter's technical code for an incoming ApplicationReceipt that is invalid, which is not answered.
     */
    public static final String INVALID_APPLICATION_RECEIPT = "E7783";

    private final String errorCode;
    private final Integer statusCode;
    /**
     * The adapter's code for a technical error, e.g. {@code E7780} for MSH, or for an invalid incoming message.
     */
    private final String technicalCode;

    public DphException(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public DphException(String errorCode, String message, Integer statusCode) {
        this(errorCode, message, statusCode, null);
    }

    public DphException(String errorCode, String message, Integer statusCode, Throwable cause) {
        this(errorCode, message, statusCode, null, cause);
    }

    public DphException(String errorCode, String message, Integer statusCode, String technicalCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
        this.technicalCode = technicalCode;
    }

    public DphException(FeilmeldingForApplikasjonskvittering feilmelding) {
        this(feilmelding, null);
    }

    public DphException(FeilmeldingForApplikasjonskvittering feilmelding, Throwable cause) {
        super(feilmelding.getNavn(), cause);
        this.errorCode = feilmelding.getVerdi();
        this.statusCode = null;
        this.technicalCode = null;
    }

    /**
     * Whether the adapter answered that the resource does not exist (any more), e.g. a business document that has
     * already been downloaded.
     */
    public boolean isNotFound() {
        return statusCode != null && (statusCode == 404 || statusCode == 410);
    }

    /**
     * Whether the adapter answered that the incoming message is invalid, so that it is rejected with an
     * ApplicationReceipt with {@link #getErrorCode()} as its error code, unless it is an ApplicationReceipt itself.
     */
    public boolean isInvalidMessage() {
        return statusCode != null && statusCode == 422
            && (INVALID_MESSAGE.equals(technicalCode) || INVALID_APPLICATION_RECEIPT.equals(technicalCode));
    }

    /**
     * Whether the adapter answered that the incoming message is an invalid ApplicationReceipt, which is not answered.
     */
    public boolean isInvalidApplicationReceipt() {
        return isInvalidMessage() && INVALID_APPLICATION_RECEIPT.equals(technicalCode);
    }
}
