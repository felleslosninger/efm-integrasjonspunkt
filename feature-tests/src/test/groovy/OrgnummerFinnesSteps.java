/**
 * Created by vat on 16.09.2015.
 */
import static org.junit.Assert.assertTrue;
import difi.GroovyExample2TestClient;

public class OrgnummerFinnesSteps {
    int orgnummer;
    boolean result;

    @cucumber.api.java.en.Given("^mottaker med organisasjonsnummer (\\d+)$")
    public void mottaker_med_organisasjonsnummer(int orgnummer) throws Throwable {
        this.orgnummer = orgnummer;
    }
    @cucumber.api.java.en.When("^mottaker finnes i adresseregister$")
    public void organisasjonsnummer_finnes() throws Throwable {
        GroovyExample2TestClient adresseregister = new GroovyExample2TestClient();
        result = adresseregister.canGetRecieveMessage(orgnummer);
    }
    @cucumber.api.java.en.Then("^skal vi få (.+) i svar$")
    public void resultat(String svar) throws Throwable {
        assertTrue(result);
    }
}
