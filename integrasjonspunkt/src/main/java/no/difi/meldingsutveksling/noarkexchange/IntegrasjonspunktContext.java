package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;

public class IntegrasjonspunktContext {
	private Avsender avsender;
	private Mottaker mottaker;
    private String jpId;
	
	public IntegrasjonspunktContext(){}
	
	public Avsender getAvsender() {
		return avsender;
	}
	public void setAvsender(Avsender avsender) {
		this.avsender = avsender;
	}
	public Mottaker getMottaker() {
		return mottaker;
	}
	public void setMottaker(Mottaker mottaker) {
		this.mottaker = mottaker;
	}

    public IntegrasjonspunktContext setJpId(String jpId) {
        this.jpId = jpId;
        return this;
    }
}
