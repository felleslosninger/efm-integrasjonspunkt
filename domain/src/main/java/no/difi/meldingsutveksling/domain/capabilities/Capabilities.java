package no.difi.meldingsutveksling.domain.capabilities;

import lombok.Data;

import java.util.List;

@Data
public class Capabilities {

    @SuppressWarnings("squid:S1700")
    private List<Capability> capabilities;
}
