package no.difi.meldingsutveksling.nextmove;

public interface HasSikkerhetsNivaa<T extends HasSikkerhetsNivaa<T>> extends DocumentAsAttachment<T> {


    T setSikkerhetsnivaa(Integer sikkerhetsnivaa);

    Integer getSikkerhetsnivaa();

}
