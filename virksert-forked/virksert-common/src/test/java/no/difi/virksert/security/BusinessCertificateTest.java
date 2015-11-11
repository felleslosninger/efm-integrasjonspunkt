package no.difi.virksert.security;

import no.difi.certvalidator.Validator;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BusinessCertificateTest {

    @Test
    public void simpleTest() throws Exception {
        Validator validatorTest = BusinessCertificate.getValidator("test");
        Validator validatorProd = BusinessCertificate.getValidator("production");
        Validator validatorProd2 = BusinessCertificate.getValidator("no.difi.virksert.scope.ProductionScope");

        Assert.assertTrue(validatorTest.isValid(getClass().getResourceAsStream("/bc-test-difi.cer")));
        Assert.assertFalse(validatorProd.isValid(getClass().getResourceAsStream("/bc-test-difi.cer")));
        Assert.assertFalse(validatorProd2.isValid(getClass().getResourceAsStream("/bc-test-difi.cer")));
    }

}
