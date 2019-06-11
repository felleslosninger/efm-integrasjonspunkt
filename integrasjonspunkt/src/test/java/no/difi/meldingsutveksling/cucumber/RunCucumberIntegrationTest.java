package no.difi.meldingsutveksling.cucumber;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import lombok.experimental.UtilityClass;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        glue = {"classpath:no.difi.meldingsutveksling.cucumber"},
        features = "classpath:cucumber",
        plugin = {"pretty", "json:target/cucumber/cucumber.json"},
        tags = {"not @Ignore"}
)
@UtilityClass
public class RunCucumberIntegrationTest {
}
