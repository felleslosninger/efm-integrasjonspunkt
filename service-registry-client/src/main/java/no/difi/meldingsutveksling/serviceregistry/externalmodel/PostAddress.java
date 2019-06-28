package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
public class PostAddress {

    private static final Set<String> NORWAY_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("NORGE", "NORWAY", "NO", "NOR")));
    public static final PostAddress EMPTY = new PostAddress();

    private String name;
    private String street;
    private String postalCode;
    private String postalArea;
    private String country;

    public PostAddress() {
        name = "";
        street = "";
        postalCode = "";
        postalArea = "";
        country = "";
    }

    public PostAddress(String name, String street, String postalCode, String postalArea, String country) {
        this.name = name;
        this.street = street;
        this.postalCode = postalCode;
        this.postalArea = postalArea;
        this.country = country;
    }

    public boolean isNorge() {
        return (country == null || "".equals(country)) || NORWAY_SET.contains(country.toUpperCase());
    }

}