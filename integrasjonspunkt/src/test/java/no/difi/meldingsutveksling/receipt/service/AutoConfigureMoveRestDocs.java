package no.difi.meldingsutveksling.receipt.service;

import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigureRestDocs
@interface AutoConfigureMoveRestDocs {
}
