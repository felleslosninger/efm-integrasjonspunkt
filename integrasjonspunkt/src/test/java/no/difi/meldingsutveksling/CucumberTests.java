package no.difi.meldingsutveksling;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.Ignore;
import org.junit.runner.RunWith;

/**
 * Entry point for cucumber tests
 */


@Ignore("Temporary ignored. Functionality is to be moved, reason queue handling.")
@RunWith(Cucumber.class)
@CucumberOptions(format = "pretty", features = "src/test/resources/")
public class CucumberTests {

}
