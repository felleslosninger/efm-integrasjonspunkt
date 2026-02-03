package no.difi.meldingsutveksling.altinnv3.token;

import no.difi.meldingsutveksling.config.Oidc;

public record TokenConfig(Oidc oidc, String exchangeUrl) { }
