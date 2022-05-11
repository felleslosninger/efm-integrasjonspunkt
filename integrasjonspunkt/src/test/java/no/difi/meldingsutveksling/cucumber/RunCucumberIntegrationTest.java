package no.difi.meldingsutveksling.cucumber;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasspathResource("cucumber")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "no.difi.meldingsutveksling.cucumber")
@ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "not @Ignore")
@SuppressWarnings("java:S2187")
public class RunCucumberIntegrationTest {
}
