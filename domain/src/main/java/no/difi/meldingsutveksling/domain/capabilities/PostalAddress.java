package no.difi.meldingsutveksling.domain.capabilities;

import lombok.Data;

@Data
public class PostalAddress {

    private String name;
    private String street;
    private String postalCode;
    private String postalArea;
    private String country;
}
