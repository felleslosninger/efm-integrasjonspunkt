package no.difi.meldingsutveksling.web.onboarding.steps;

public interface Step {

    enum ActionType { VERIFY, CONFIRM }
    record StepInfo(String name, String title, String description, String dialogText, String buttonText, boolean required, boolean completed) { }

    String getName();
    boolean isRequired();
    boolean isCompleted();
    void executeAction(ActionType action);
    StepInfo getStepInfo();

}
