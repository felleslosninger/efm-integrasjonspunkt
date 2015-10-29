package no.difi.virksert.security;

import no.difi.certvalidator.Validator;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BusinessCertificateTest {

    @Test
    public void simpleTest() throws Exception {
        Validator validatorTest = BusinessCertificate.getValidator("test",
                new String[]{"buypass-root", "commfides-root"},
                new String[]{"buypass-intermediate", "commfides-intermediate"});

        Validator validatorProd = BusinessCertificate.getValidator("production",
                new String[]{"buypass-root", "commfides-root"},
                new String[]{"buypass-intermediate", "commfides-intermediate"});

        Assert.assertTrue(validatorTest.isValid(getClass().getResourceAsStream("/bc-test-difi.cer")));
        Assert.assertFalse(validatorProd.isValid(getClass().getResourceAsStream("/bc-test-difi.cer")));
    }

}
