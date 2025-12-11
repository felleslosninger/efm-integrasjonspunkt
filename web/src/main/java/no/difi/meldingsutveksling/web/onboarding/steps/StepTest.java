package no.difi.meldingsutveksling.web.onboarding.steps;

import org.springframework.stereotype.Service;

@Service
public class StepTest implements Step {

    private boolean STEP_COMPLETED = false;
    private boolean hasSentTestMessage = false;

    @Override
    public String getName() {
        return "testflow";
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public boolean isCompleted() {
        return STEP_COMPLETED;
    }

    @Override
    public void verify(String value) {
        STEP_COMPLETED = !STEP_COMPLETED;
        hasSentTestMessage = true;
    }

    @Override
    public StepInfo getStepInfo() {

        var dialogText = STEP_COMPLETED ?
            "Testmelding har blitt sendt." :
            hasSentTestMessage ?
                    "Sending av testmelding feilet, sjekk feilloggen." :
                    "Sender en testmelding til deg selv.";

        return new StepInfo(
                getName(),
                "Testflyt",
                "Send en testmelding til deg selv og verifiser mottak før produksjonssetting.",
                dialogText,
                isCompleted() ? "Lukk" : "Send testmelding",
                isRequired(),
                isCompleted()
        );

    }

}
