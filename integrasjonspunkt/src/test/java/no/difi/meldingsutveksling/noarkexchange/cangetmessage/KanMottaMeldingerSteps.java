package no.difi.meldingsutveksling.noarkexchange.cangetmessage;

import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import no.difi.meldingsutveksling.adresseregister.client.CertificateNotFoundException;
import no.difi.meldingsutveksling.eventlog.EventLog;
import no.difi.meldingsutveksling.noark.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.IntegrasjonspunktImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.AddressType;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.GetCanReceiveMessageResponseType;
import no.difi.meldingsutveksling.services.AdresseregisterService;
import sun.security.x509.X509CertImpl;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KanMottaMeldingerSteps {

    private IntegrasjonspunktImpl integrasjonspunkt;
    private AdresseregisterService adresseRegister;
    private GetCanReceiveMessageResponseType responseType;
    private NoarkClient mshClient;

    @Before
    public void setup() {
        integrasjonspunkt = new IntegrasjonspunktImpl();
        adresseRegister = mock(AdresseregisterService.class);
        mshClient = mock(NoarkClient.class);
        integrasjonspunkt.setEventLog(mock(EventLog.class));
        integrasjonspunkt.setAdresseRegister(adresseRegister);
        integrasjonspunkt.setMshClient(mshClient);
    }


    @Given("^virksomhet (.+) i Adresseregisteret$")
    public void virksomhet_i_Adresseregisteret(String finnes) throws Throwable {
        if("finnes".equals(finnes)) {
            when(adresseRegister.getCertificate(any(String.class))).thenReturn(new X509CertImpl());
        } else if("finnes ikke".equals(finnes)) {
            when(adresseRegister.getCertificate(any(String.class))).thenThrow(CertificateNotFoundException.class);
        }
    }

    @And("^virksomhet (.+) i MSH$")
    public void virksomhet_finnes_i_MSH(String finnes) throws Throwable {
        if("finnes".equals(finnes)) {
            when(mshClient.canGetRecieveMessage(any(String.class))).thenReturn(true);
        } else if("finnes ikke".equals(finnes)) {
            when(mshClient.canGetRecieveMessage(any(String.class))).thenReturn(false);
        }
    }

    @When("^vi spør integrasjonspunktet om virksomhet kan motta meldinger$")
    public void vi_spør_integrasjonspunktet_om_virksomhet_kan_motta_meldinger() throws Throwable {
        // Express the Regexp above with the code you wish you had
        GetCanReceiveMessageRequestType request = new GetCanReceiveMessageRequestType();
        AddressType addressType = new AddressType();
        addressType.setOrgnr("12345678");
        request.setReceiver(addressType);
        responseType = integrasjonspunkt.getCanReceiveMessage(request);
    }

    @Then("^skal vi få (.+) om at de kan motta meldinger$")
    public void skal_vi_få_svar_om_at_de_kan_motta_meldinger(String svar) throws Throwable {
        if("sann".equals(svar)) {
            assertTrue(responseType.isResult());
        } else if("usann".equals(svar)) {
            assertFalse(responseType.isResult());
        }

    }

}
