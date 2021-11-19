package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.clock.TestClock;

@RequiredArgsConstructor
public class ClockSteps {

    private final TestClock clock;

    @Before
    public void before() {
        clock.reset();
    }

    @And("^the clock is \"([^\"]*)\"$")
    public void theClockIs(String in) {
        clock.setActive(in);
    }
}
