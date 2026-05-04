package no.difi.meldingsutveksling.web.onboarding;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.FrontendFunctionality;
import no.difi.meldingsutveksling.web.onboarding.steps.*;
import no.difi.meldingsutveksling.web.onboarding.steps.Step.ActionType;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class OnboardingController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(OnboardingController.class);

    @Inject StepScopes step1;
    @Inject StepSystem step2;
    @Inject StepSystembruker step3;
    @Inject StepKonfigurer step4;
    @Inject StepPaavegneav step5;
    @Inject StepTest step6;

    @Inject FrontendFunctionality ff;

    private List<Step> steps;

    @PostConstruct
    void init() {
        steps = List.of(step1, step2, step3, step4, step5, step6);
    }

    @GetMapping("/onboarding")
    public String onboarding(Model model) {
        model.addAttribute("checklist", steps.stream().map(Step::getStepInfo).toList());
        model.addAttribute("contacts", contacts());
        model.addAttribute("resources", resources());
        return "onboarding";
    }

    record Contact(String role, String name, String email) { }

    record ResourceLink(String label, String url) { }

    private List<Contact> contacts() {
        return List.of(
                new Contact("Integrasjonspunkt", "Forvaltning eFormidling", "servicedesk@digdir.no"),
                new Contact("Altinn", "Altinn sporing og forvaltning", "servicedesk@altinn.no"),
                new Contact("ELMA", "Adresseregister", "elma@digdir.no")
        );
    }

    private List<ResourceLink> resources() {
        return List.of(
                new ResourceLink("Altinn Formidling (DPO) dokumentasjon", "https://docs.altinn.studio/nb/broker/"),
                new ResourceLink("Altinn Melding (DPV) dokumentasjon", "https://docs.altinn.studio/nb/correspondence/"),
                new ResourceLink("Digital post til innbyggere (DPI)", "https://www.digdir.no/felleslosninger/digital-postkasse-til-innbyggere/775"),
                new ResourceLink("Maskinporten klientoppsett", "https://docs.digdir.no/docs/Maskinporten/maskinporten_sjolvbetjening_web.html")
        );
    }

    @GetMapping("/onboarding/token/{meldingstjeneste}")
    public ResponseEntity<?> accessToken(@PathVariable String meldingstjeneste) {
        log.info("Fetching accesstoken for meldingstjeneste : {}", meldingstjeneste);
        if (meldingstjeneste == null) meldingstjeneste = "";
        String response = null;
        if ("DPO".equalsIgnoreCase(meldingstjeneste)) response = ff.dpoAccessToken(List.of("altinn:broker.read","altinn:broker.write"));
        if ("DPV".equalsIgnoreCase(meldingstjeneste)) response = ff.dpvAccessToken();
        if ("DPI".equalsIgnoreCase(meldingstjeneste)) response = ff.dpiAccessToken();
        if (response == null) response = "Får ikke tak i token, ukjent meldingstjeneste '%s'".formatted(meldingstjeneste);
        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @GetMapping("/onboarding/dialog/{dialog}")
    public ResponseEntity<?> dialogDetails(@PathVariable String dialog) {
        log.info("Fetching dialog: {}", dialog);
        var step = findOnboardingStep(dialog);
        return ResponseEntity.ok(step.getStepInfo());
    }

    @ResponseBody
    @PostMapping("/onboarding/dialog/{dialog}/confirm")
    public ResponseEntity<?> confirmDialog(@PathVariable String dialog, @RequestBody(required = false) Map<String, String> params) {
        log.info("Confirming dialog: {}", dialog);
        var step = findOnboardingStep(dialog);
        step.setParams(params);
        step.executeAction(ActionType.CONFIRM);
        return ResponseEntity.ok(step.getStepInfo());
    }

    @ResponseBody
    @PostMapping("/onboarding/dialog/{dialog}/cancel")
    public ResponseEntity<?> cancelDialog(@PathVariable String dialog) {
        log.info("Cancelling dialog: {}", dialog);
        var step = findOnboardingStep(dialog);
        step.executeAction(ActionType.CANCEL);
        return ResponseEntity.ok(step.getStepInfo());
    }

    private Step findOnboardingStep(String dialog) {
        return steps.stream().filter(step -> step.getName().equals(dialog)).findFirst().get();
    }

}
