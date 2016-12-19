package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import org.springframework.beans.factory.annotation.Autowired;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

public class EDUCoreConverterImpl implements EDUCoreConverter {
    @Autowired
    IntegrasjonspunktProperties properties;

    public Forsendelse convert(EDUCore domainMessage) {
        byte[] data = new byte[1024];
        final Dokument dokument = Dokument.builder().withData(new DataHandler(new ByteArrayDataSource(data, "pdf"))).build();
        final Mottaker mottaker = Organisasjon.builder().build();
        final Printkonfigurasjon printkonfigurasjon = Printkonfigurasjon.builder().build();
        Forsendelse forsendelse = Forsendelse.builder()
                .withTittel("Tittel")
                .withAvgivendeSystem(properties.getNoarkSystem().getType())
                .withDokumenter(dokument)
                .withMottaker(mottaker)
                .withPrintkonfigurasjon(printkonfigurasjon)
                .withKrevNiva4Innlogging(true)
                .withKryptert(true).build();
        return forsendelse;
    }
}
