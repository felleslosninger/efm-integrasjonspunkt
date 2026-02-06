package no.difi.meldingsutveksling.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;

/**
 * Aspekt som legg til rette for meir realistisk ytelsestest av masseutsending til innbyggere.
 * DPI tilbyr testoppsett med svært få mottakere per per 28. mai 2021.
 *
 * Test med få mottakere medfører urealistisk ytelse for adresseoppslag som på grunn av cache berre blir gjort 1 gong
 * istadenfor for eksempel 1 000 000 gonger.
 *
 * For å muliggjere meir realistisk ytelsestest tømmer me cache ved oppretting av melding, slik at mottaker framstår som
 * ny for alle utsendinger (sjølv om det er samme mottaker). Alle utsendinger vil dermed medføre adresseoppslag og gir
 * derfor ein meir realistisk ytelsestest.
 *
 * Aspektet er berre satt opp for endepunktet for oppretting og sending i samme operasjon
 *
 * Aspektet blir berre aktivert for Spring profil "yt"
 *
 */
@Configuration
@EnableAspectJAutoProxy // Enables defining aspects using @Aspect annotations
@Profile("yt")
@Aspect
@RequiredArgsConstructor
public class EvictCacheBeforeCreateAndSendMessage {

    private final CacheManager cacheManager;

    @Before("execution(* no.difi.meldingsutveksling.nextmove.v2.NextMoveMessageOutController.createAndSendMessage(..)) && args(sbd, ..)")
    public void evictCacheBeforeCreateAndSendMessage(StandardBusinessDocument sbd) {
        System.out.println("Evicting cache");
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_LOAD_IDENTIFIER_RESOURCE);
        if (cache != null) {
            cache.invalidate();
        }
    }
}
