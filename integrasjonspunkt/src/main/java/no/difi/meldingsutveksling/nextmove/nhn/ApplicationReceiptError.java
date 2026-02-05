package no.difi.meldingsutveksling.nextmove.nhn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApplicationReceiptError(FeilmeldingForApplikasjonskvittering type,
                                      String details) {


}
