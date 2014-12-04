package no.difi.meldingsutveksling.noarkexchange;

import no.difi.meldingsutveksling.domain.Avsender;
import no.difi.meldingsutveksling.domain.Mottaker;

public class KnutepunktContext {
	private Avsender avsender;
	private Mottaker mottaker;
    private String jpId;
	
	public KnutepunktContext(Avsender avsender, Mottaker mottaker) {
		super();
		this.avsender = avsender;
		this.mottaker = mottaker;
	}
	
	public KnutepunktContext(){}
	
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

    public String getJpId() {
        return jpId;
    }

    public KnutepunktContext setJpId(String jpId) {
        this.jpId = jpId;
        return this;
    }
}
