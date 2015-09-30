package difi.steps;

import cucumber.api.PendingException;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;
import cucumber.api.java.en.Then;

import difi.BestEduTestMessageFactory;
import difi.CertificatesClient;
import difi.TestClient;
import groovy.lang.Writable;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import static org.junit.Assert.assertEquals;


/**
 * Created by vat on 25.09.2015.
 */
public class SikreMeldingSteps {

    private String soapResponse;
    private String bestEduMessage;
    private String avsender, mottaker;
    private CompositeConfiguration configuration;
    private CertificatesClient client;

    @Before
    public void setup() throws ConfigurationException {
        this.configuration = new CompositeConfiguration();
        configuration.addConfiguration(new SystemConfiguration());
        configuration.addConfiguration(new PropertiesConfiguration("test-configuration.properties"));
        this.client = new CertificatesClient(configuration.getString("adresseregister.url"));
    }

    @And("^mottaker med orgnummer (\\d+) har gyldig sertifikat$")
    public void mottaker_med_orgnummer_har_gyldig_sertifikat(String arg1) throws Throwable {
        mottaker = arg1;
        String certificate = "-----BEGIN CERTIFICATE-----\nMIID+jCCAuKgAwIBAgIEcBn/CDANBgkqhkiG9w0BAQsFADBeMRIwEAYDVQQKEwlE\naWZpIHRlc3QxEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0\nIHZpcmtzb21oZXRzc2VydGlmaWF0IGludGVybWVkaWF0ZTAeFw0xNTA4MTgxMDUz\nNDZaFw0xNzA5MTgxMDUzNDZaMD0xEjAQBgNVBAUTCTkxMDA3NTkxODEnMCUGA1UE\nAxMeRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlmaWF0MIIBIjANBgkqhkiG9w0B\nAQEFAAOCAQ8AMIIBCgKCAQEA90i1/3O38Glmts65ArxE9TdIdaa6MgNXOca+uz2o\n9kYb1nABBPWbPFOotwIm3o+U7v0lqIAukbH22bb22DsM6WymG0yZAGcHZuou3adg\nJBC36DF3nLc56sTZGpMOLQ9KpFOAjRBXZVqOYgrHo6bhSSUX7JKWEhnjdRrSiJ7I\n7GB9pD9DawR/3NJdy8nNTgMa2PZrYEjrokrcIFd36FiKRMJQKbmfF9gvGqYNSvud\n7XJ8v/xgbHCqPn5yr4/ZZLX8wRmLcSclNEEGvr2D9w4QWTNshzP8rZTsOExCwdSn\nPGTzzlpG/JTYDhcqISlytASiFyYZY6D8UBHFuGyUzgxvMQIDAQABo4HgMIHdMIGJ\nBgNVHSMEgYEwf4AUugESfzZfoj5AU8tdBpbRFnf/DoehYqRgMF4xEjAQBgNVBAoT\nCURpZmkgdGVzdDESMBAGA1UEBRMJOTkxODI1ODI3MTQwMgYDVQQDEytESUZJIHRl\nc3Qgdmlya3NvbWhldHNzZXJ0aWZpYXQgaW50ZXJtZWRpYXRlggMbVIYwHQYDVR0O\nBBYEFGrpRWdcH7OMqUSa8GkT1vQzkSwxMAkGA1UdEwQCMAAwFQYDVR0gBA4wDDAK\nBghghEIBAQEBZDAOBgNVHQ8BAf8EBAMCBLAwDQYJKoZIhvcNAQELBQADggEBAEbC\nKw/1MEDFKCzE5qcrHx9l9L8QIo1HrpUpzsjEsTEco98r2wmcuSMTzi3bQc63T2xc\nG6eEySBlpjyZG7xHxGHWGNwQqgvx/vD6JUgT03tcMjt4TckrtglmHv2trOv2jxip\nDv7HsADOCxB2O+1j5otdZhCsPW9dMV4yOdygOM7MC6ocELIGfyGOYd12+HeyHBDB\nu5GRGcbj4QI1omJ9HGHz0VUxgJbH42L7H+Hl5rRv5Bp743ILIJmTIMljDEHaAPXz\nVbYRqzGbKmTY40I75dS2ds00uKdzzxJBnogaTok+gvcMFZOJsV05L99JoirkuS4b\ne2FbJoQnBLqnxMup5g4=\n-----END CERTIFICATE-----";
        client = new CertificatesClient(configuration.getString("adresseregister.url"));
        if(!client.exists(mottaker)) {
            client.addOrganization(mottaker, certificate);
        }
        throw new PendingException();
    }

    @Given("^avsender med orgnummer (\\d+) har gyldig sertifikat$")
    public void avsender_med_orgnummer_har_gyldig_sertifikat(String arg1) throws Throwable {
        avsender = arg1;
        String certificate = "-----BEGIN CERTIFICATE-----\nMIID+jCCAuKgAwIBAgIEYkWeKzANBgkqhkiG9w0BAQsFADBeMRIwEAYDVQQKEwlE\naWZpIHRlc3QxEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0\nIHZpcmtzb21oZXRzc2VydGlmaWF0IGludGVybWVkaWF0ZTAeFw0xNTA4MTgxMDUz\nNDZaFw0xNzA5MTgxMDUzNDZaMD0xEjAQBgNVBAUTCTkxMDA3NTkxODEnMCUGA1UE\nAxMeRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlmaWF0MIIBIjANBgkqhkiG9w0B\nAQEFAAOCAQ8AMIIBCgKCAQEA3Ow3dln4ThglDFvZKfL1GBKGRFmDaSB7JLl/+1Qy\nHUTyX1r16mSL/wwMZLnGMiplwSEyK5EXq00ASQXmm0BKYyVRvOP+WrUDukyyxc0H\nb7sAtM/W5gvViRyGgVTx+vf3zuN3J+OClUOXx/1kIEI6sPue+N/ySLPFoFXcQqWT\nVnz4qGYrOuMyGXp9BHFk64yS2TjiVh7uVbZbPczI4/wQWglXlZP2ISWn5JdAXTEh\nvOiWI2j3d3kqTeOKAx6c631ziUiWmZKlLWkTesASFSLw7ITII2zBPx8nJVD2sOJ+\nRvF3cipSrRyPmgHVZzqA25NGdlBDNmOpTLOmHM334kQqaQIDAQABo4HgMIHdMIGJ\nBgNVHSMEgYEwf4AUugESfzZfoj5AU8tdBpbRFnf/DoehYqRgMF4xEjAQBgNVBAoT\nCURpZmkgdGVzdDESMBAGA1UEBRMJOTkxODI1ODI3MTQwMgYDVQQDEytESUZJIHRl\nc3Qgdmlya3NvbWhldHNzZXJ0aWZpYXQgaW50ZXJtZWRpYXRlggMbVIYwHQYDVR0O\nBBYEFM9tT1foBAPBi1EUUbx7tW7c++PyMAkGA1UdEwQCMAAwFQYDVR0gBA4wDDAK\nBghghEIBAQEBZDAOBgNVHQ8BAf8EBAMCBLAwDQYJKoZIhvcNAQELBQADggEBAH74\nvPE0/I3b+wiAf5fYg64uUWJWz4kPCncmDJ1Ba7w0YIXmjBtn77OFThyUA3VFFKk\nK1afF3GGRWD7/K+85MSsyQkbySRC60nLAAx8KlQFmW3ael10yowm6QYCBpu/GtsN\nNr3U+X0EW3rW+0lUWV2/HpCVKvVDnfmlATJFcBHBHULDmSQ7FOZ/wKqEG2n1NsEF\ne7UqUsxWyunnxEmThDZL5PZ9Y8cOZXiTOqpXk7jO7u6gAYBPDq4qAEz7SOPvK2Fx\n6E/ZiDAvSB0NR3yQpEYj4ieDQIBkvNTze87agjGJ7vLtt2Wo2aVqFYDkd9tBMaBs\nDIwvchwI+Hf0BzE7nvM=\n-----END CERTIFICATE-----";
        client = new CertificatesClient(configuration.getString("adresseregister.url"));
        if(!client.exists(avsender)) {
            client.addOrganization(avsender, certificate);
        }
        throw new PendingException();
    }

    @When("^vi sender melding$")
    public void vi_sender_melding_1() throws Throwable {
        Writable message = BestEduTestMessageFactory.createMessage(128);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(new BufferedWriter(new OutputStreamWriter(outputStream)));
        bestEduMessage = message.toString();

        TestClient testClient = new TestClient(configuration.getString("integrasjonspunkt.url"));
        soapResponse = testClient.putMessage(avsender, mottaker, bestEduMessage);
        throw new PendingException();
    }

    @Then("^vi skal fa svar om at melding har blitt formidlet$")
    public void vi_skal_fa_svar_om_at_melding_har_blitt_formidlet() throws Throwable {
        assertEquals("OK", soapResponse);
        throw new PendingException();
    }
}
