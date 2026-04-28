package no.difi.meldingsutveksling.web.onboarding.steps;

import java.util.Map;

public interface Step {

    enum ActionType { VERIFY, CONFIRM, CANCEL }

    record StepInfo(
        String name,
        String title,
        String description,
        String dialogText,
        String buttonText,
        boolean required,
        boolean completed,
        boolean enableCancelButton, // cancel button will call executeAction(CANCEL) before closing dialog
        boolean enableActionButton, // action button will be disabled or enabled
        boolean showInformationAfterCompletion
    ) { }

    String getName();
    boolean isRequired();
    boolean isCompleted();
    default void setParams(Map<String, String> params) { }
    void executeAction(ActionType action);
    StepInfo getStepInfo();

}
