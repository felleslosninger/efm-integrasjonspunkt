package no.difi.virksert.server.task;

import no.difi.certvalidator.Validator;
import no.difi.virksert.server.model.Registration;
import no.difi.virksert.server.repository.RegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Component
public class ValidationTask {

    private static Logger logger = LoggerFactory.getLogger(ValidationTask.class);

    @Autowired
    private Validator validator;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Scheduled(cron = "0 0 3 * * *")
    // @Scheduled(fixedDelay = 1000)
    public void run() {
        logger.info("Validating registered certificates.");

        for (Registration registration : registrationRepository.listValid()) {
            if (!validator.isValid(registration.getCertificate())) {
                registration.setRevoked(System.currentTimeMillis());
                registrationRepository.save(registration);
            }
        }
    }

}
