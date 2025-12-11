package no.difi.meldingsutveksling.web;

import java.util.List;

public class FrontendFunctionalityFaker implements FrontendFunctionality {

    @Override
    public boolean isFake() {
        return true;
    }

    @Override
    public List<String> getChannelsEnabled() {
        return List.of("DPO", "DPV", "DPI", "DPF", "DPFIO", "DPE");
    }

}
