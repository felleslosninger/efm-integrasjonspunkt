package no.difi.meldingsutveksling.dpi.client.internal;

import no.difi.certvalidator.Validator;
import no.difi.certvalidator.ValidatorLoader;
import no.difi.certvalidator.api.CertificateValidationException;
import no.difi.certvalidator.lang.ValidatorParsingException;
import no.difi.meldingsutveksling.dpi.client.internal.domain.Mode;
import no.difi.meldingsutveksling.dpi.client.internal.domain.RecipePath;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;

public class BusinessCertificateValidator {

    private static final String VALIDATION_OF_BUSINESS_CERTIFICATE_FAILED = "Validation of business certificate failed!";

    /**
     * Holds the actual certificate validator.
     */
    private final Validator validator;

    /**
     * Use of {@link Mode} to load the certificate validator using resources part of this package.
     *
     * @param mode One of the modes part of this package.
     * @return Validator for validation of business certificates.
     * @throws LoadingException when loading of validator is unsuccessful.
     */
    public static BusinessCertificateValidator of(Mode mode) throws LoadingException {
        return of((Enum<Mode>) mode);
    }

    /**
     * Use of annotated enum to load the certificate validator. This is overloaded by #of(Mode) to allow for easy
     * detection in IDE of the {@link Mode} enums.
     * <p>
     * Enums used in this method must be annotated with {@link RecipePath}.
     *
     * @param mode Some enum annotated with {@link RecipePath}
     * @return Validator for validation of business certificates.
     * @throws LoadingException when loading of validator is unsuccessful.
     */
    public static BusinessCertificateValidator of(Enum<?> mode) throws LoadingException {
        return of(pathFromEnum(mode));
    }

    /**
     * Loads a certificate validator by providing mode as {@link String}. When mode is not detected is the value
     * expected to be the path to validator recipe in class path.
     *
     * @param modeString Mode as string.
     * @return Validator for validation of business certificates.
     * @throws LoadingException when loading of validator is unsuccessful.
     */
    public static BusinessCertificateValidator of(String modeString) throws LoadingException {
        String path = Mode.of(modeString)
                .map(BusinessCertificateValidator::pathFromEnum)
                .orElse(modeString);

        return new BusinessCertificateValidator(path);
    }

    /**
     * Returns path found in {@link RecipePath} annotation on a given {@link Enum}.
     *
     * @param mode Some enum.
     * @return Path from {@link RecipePath} annotation.
     */
    private static String pathFromEnum(Enum<?> mode) {
        try {
            return mode.getClass().getField(mode.name()).getAnnotation(RecipePath.class).value();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("Something is terribly wrong.", e);
        }
    }

    /**
     * Loads the certificate validator by using the path to the recipe file found in class path.
     *
     * @param path Path to recipe file in class path.
     * @throws LoadingException when loading of validator is unsuccessful.
     */
    private BusinessCertificateValidator(String path) throws LoadingException {
        try (InputStream inputStream = getClass().getResourceAsStream(path)) {
            this.validator = ValidatorLoader.newInstance().build(inputStream);
        } catch (IOException | ValidatorParsingException e) {
            throw new LoadingException("Unable to load certificate validator.", e);
        }
    }

    /**
     * Validate certificate.
     *
     * @param certificate Certificate as a {@link X509Certificate} object.
     * @throws Exception validation failed.
     */
    public void validate(X509Certificate certificate) throws Exception {
        try {
            validator.validate(certificate);
        } catch (CertificateValidationException e) {
            throw new Exception(VALIDATION_OF_BUSINESS_CERTIFICATE_FAILED, e);
        }
    }

    /**
     * Validate certificate.
     *
     * @param certificate Certificate as a byte array.
     * @throws Exception validation failed.
     */
    public void validate(byte[] certificate) throws Exception {
        try {
            validator.validate(certificate);
        } catch (CertificateValidationException e) {
            throw new Exception(VALIDATION_OF_BUSINESS_CERTIFICATE_FAILED, e);
        }
    }

    /**
     * Validate certificate.
     *
     * @param inputStream Certificate from an {@link InputStream}.
     * @throws Exception validation failed.
     */
    public void validate(InputStream inputStream) throws Exception {
        try {
            validator.validate(inputStream);
        } catch (CertificateValidationException e) {
            throw new Exception(VALIDATION_OF_BUSINESS_CERTIFICATE_FAILED, e);
        }
    }

    public static class Exception extends RuntimeException {
        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class LoadingException extends RuntimeException {
        public LoadingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
