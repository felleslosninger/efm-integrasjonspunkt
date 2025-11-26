package no.difi.meldingsutveksling.nextmove;

public interface HasSikkerhetsNivaa<T extends HasSikkerhetsNivaa<T>> extends DocumentAsAttachment<T> {


    public T setSikkerhetsnivaa(Integer sikkerhetsnivaa);

    public Integer getSikkerhetsnivaa();
}
