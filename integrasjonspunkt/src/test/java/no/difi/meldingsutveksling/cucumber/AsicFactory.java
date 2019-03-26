package no.difi.meldingsutveksling.cucumber;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.IntegrasjonspunktNokkel;
import no.difi.meldingsutveksling.dokumentpakking.domain.Archive;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.dokumentpakking.service.CreateAsice;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("cucumber")
@RequiredArgsConstructor
public class AsicFactory {

    private final IntegrasjonspunktNokkel keyInfo;
    private final ObjectProvider<CmsUtil> cmsUtilProvider;

    @SneakyThrows
    byte[] getAsic(Message message) {
        Archive asice = new CreateAsice()
                .createAsice(message.getAttachments(),
                        keyInfo.getSignatureHelper(),
                        message.getAvsender(),
                        message.getMottaker());

        return cmsUtilProvider.getIfAvailable().createCMS(asice.getBytes(), keyInfo.getX509Certificate());
    }
}
