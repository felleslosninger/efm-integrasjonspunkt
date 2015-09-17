/**
 * Created by vat on 16.09.2015.
 */
import static org.junit.Assert.assertFalse;
import difi.GroovyExample2TestClient;

public class OrgnummerFinnesIkkeSteps {
    int orgnummer;
    boolean result;

    @cucumber.api.java.en.Given("^mottaker med organisasjonsnummer (\\d+)$")
    public void mottaker_med_organisasjonsnummer(int orgnummer) {
        this.orgnummer = orgnummer;
    }

    @cucumber.api.java.en.When("^organisasjonsnummer ikke finnes$")
    public void organisasjonsnummer_ikke_finnes() {
        GroovyExample2TestClient adresseregister = new GroovyExample2TestClient();
        result = adresseregister.canGetRecieveMessage(orgnummer);
    }
    @cucumber.api.java.en.Then("^skal vi få (.+) i svar$")
     public void skal_vi_få_resultat_i_svar(String svar) {
        assertFalse(result);
    }
}
