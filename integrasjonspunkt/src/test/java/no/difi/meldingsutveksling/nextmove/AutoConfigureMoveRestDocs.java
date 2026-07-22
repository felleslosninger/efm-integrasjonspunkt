package no.difi.meldingsutveksling.nextmove;

import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigureRestDocs
@interface AutoConfigureMoveRestDocs {
}
