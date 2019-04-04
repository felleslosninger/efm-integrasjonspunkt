package no.difi.meldingsutveksling.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NorwegianIndividualNumberValidator implements ConstraintValidator<NorwegianIndividualNumber, CharSequence> {

    private static final int[] FACTORS1 = {3, 7, 6, 1, 8, 9, 4, 5, 2};
    private static final int[] FACTORS2 = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    public void initialize(NorwegianIndividualNumber constraint) {
        // NOOP
    }

    public boolean isValid(CharSequence s, ConstraintValidatorContext context) {
        if (s == null || s.length() != 11) {
            return true;
        }

        return checksum(s, FACTORS1) == digit(s.charAt(9))
                && checksum(s, FACTORS2) == digit(s.charAt(10));
    }

    private int checksum(CharSequence in, int[] factors) {
        int checksum = 11 - (sum(in, factors) % 11);
        return (checksum == 11) ? 0 : checksum;
    }

    private int sum(CharSequence in, int[] factors) {
        int sum = 0;
        for (int i = 0, l = factors.length; i < l; ++i) {
            sum += digit(in.charAt(i)) * factors[i];
        }
        return sum;
    }

    private int digit(char c) {
        return Character.digit(c, 10);
    }
}
