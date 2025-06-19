package no.difi.meldingsutveksling.altinnv3.DPV;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.digdir.altinn3.correspondence.model.InitializeCorrespondencesExt;
import org.springframework.stereotype.Component;

@Slf4j
@Component
//@ConditionalOnProperty(name = "difi.move.feature.enableDPO", havingValue = "true")
@RequiredArgsConstructor
public class CorrespondanceApiClient {

    // send
    // receive

    InitializeCorrespondencesExt request = new InitializeCorrespondencesExt();

}
