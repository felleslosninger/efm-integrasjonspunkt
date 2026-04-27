package no.difi.meldingsutveksling.web.onboarding.steps;

import java.util.Map;

public interface Step {

    enum ActionType { VERIFY, CONFIRM }
    record StepInfo(String name, String title, String description, String dialogText, String buttonText, boolean required, boolean completed, boolean enableActionButton, boolean showInformationAfterCompletion) { }

    String getName();
    boolean isRequired();
    boolean isCompleted();
    default void setParams(Map<String, String> params) { }
    void executeAction(ActionType action);
    StepInfo getStepInfo();

}
