package no.difi.meldingsutveksling.cucumber;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasspathResource("cucumber")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "no.difi.meldingsutveksling.cucumber")
public class RunCucumberIntegrationTest {
}
