package no.difi.meldingsutveksling.web.onboarding;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import no.difi.meldingsutveksling.web.onboarding.steps.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class OnboardingController {

    @Inject StepScopes step1;
    @Inject StepSystem step2;
    @Inject StepSystembruker step3;
    @Inject StepKonfigurer step4;
    @Inject StepElma step5;
    @Inject StepTest step6;

    private List<Step> steps;

    @PostConstruct
    void init() {
        steps = List.of(step1, step2, step3, step4, step5, step6);
        // FIXME do some checks on startup steps.stream().filter(Step::isRequired).forEach(s -> s.verify("init"));
        // FIXME is there some issues with the
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

    @GetMapping("/onboarding/dialog/{dialog}")
    public ResponseEntity<?> dialogDetails(@PathVariable String dialog) {
        System.out.println("Fetching dialog: " + dialog);
        var step = findOnboardingStep(dialog);
        return ResponseEntity.ok(step.getStepInfo());
    }

    @PostMapping("/onboarding/dialog/{dialog}/confirm")
    public ResponseEntity<?> confirmDialog(@PathVariable String dialog) {
        System.out.println("Confirming dialog: " + dialog);
        var step = findOnboardingStep(dialog);
        step.verify("data");
        return ResponseEntity.ok(step.getStepInfo());
    }

    private Step findOnboardingStep(String dialog) {
        return steps.stream().filter(step -> step.getName().equals(dialog)).findFirst().get();
    }

}
