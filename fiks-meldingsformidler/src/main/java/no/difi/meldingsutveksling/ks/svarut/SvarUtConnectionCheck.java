package no.difi.meldingsutveksling.ks.svarut;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@RequiredArgsConstructor
public class SvarUtConnectionCheck {

    private final SvarUtService svarUtService;

//    @PostConstruct
//    public void checkTheConnection() {
//        try {
//            List<String> forsendelseTyper = svarUtService.retreiveForsendelseTyper();
//            if (forsendelseTyper.isEmpty()) {
//                throw new NextMoveRuntimeException("Couldn't retrieve forsendelse typer from SvarUt");
//            }
//        } catch (Exception e) {
//            throw new NextMoveRuntimeException("Couldn't retrieve forsendelse typer from SvarUt");
//        }
//    }
}
