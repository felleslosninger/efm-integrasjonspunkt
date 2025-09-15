package no.difi.meldingsutveksling.altinnv3.token;

public interface TokenExchangeService {
    String exchangeToken(String token, String exchangeUrl);
}
