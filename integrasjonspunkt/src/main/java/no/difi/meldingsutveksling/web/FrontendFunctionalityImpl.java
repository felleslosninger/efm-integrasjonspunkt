package no.difi.meldingsutveksling.web;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class FrontendFunctionalityImpl implements FrontendFunctionality {

    private final IntegrasjonspunktProperties props;

    @Override
    public List<String> getChannelsEnabled() {
        var channels = new ArrayList<String>();
        if (props.getFeature().isEnableDPO()) channels.add("DPO");
        if (props.getFeature().isEnableDPV()) channels.add("DPV");
        if (props.getFeature().isEnableDPI()) channels.add("DPI");
        if (props.getFeature().isEnableDPF()) channels.add("DPF");
        if (props.getFeature().isEnableDPFIO()) channels.add("DPFIO");
        if (props.getFeature().isEnableDPE()) channels.add("DPE");
        return channels;
    }

}
