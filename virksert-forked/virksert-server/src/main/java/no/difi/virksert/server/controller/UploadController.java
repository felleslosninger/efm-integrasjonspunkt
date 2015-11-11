package no.difi.virksert.server.controller;

import no.difi.certvalidator.Validator;
import no.difi.virksert.server.form.UploadForm;
import no.difi.virksert.server.logic.RegistrationService;
import no.difi.virksert.server.model.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.cert.X509Certificate;

@Controller
@RequestMapping("/upload")
public class UploadController {

    private static Logger logger = LoggerFactory.getLogger(UploadController.class);

    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private Validator validator;

    @RequestMapping(method = RequestMethod.GET)
    public String viewForm(ModelMap modelMap) {
        modelMap.put("uploadForm", new UploadForm());
        return "upload";
    }

    @RequestMapping(method = RequestMethod.POST)
    public String submitForm(UploadForm uploadForm, RedirectAttributes redirectAttributes) {
        try {
            X509Certificate certificate = Validator.getCertificate(uploadForm.getFile().getInputStream());
            validator.validate(certificate);

            Registration registration = registrationService.save(certificate);
            logger.info("Certificate for '{}' saved.", registration.getIdentifier());

            return String.format("redirect:/cert/%s", registration.getIdentifier());
        } catch (Exception e) {
            logger.warn(e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/upload";
        }
    }
}
