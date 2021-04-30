package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.HashBiMap;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.util.Set;

public class BusinessMessageUtil {

    private static final HashBiMap<String, Class<?>> businessMessages = HashBiMap.create();

    static {
        Reflections reflections = new Reflections("no.difi.meldingsutveksling.nextmove",
            new TypeAnnotationsScanner(), new SubTypesScanner());
        reflections.getTypesAnnotatedWith(NextMoveBusinessMessage.class).forEach(c ->
            businessMessages.put(c.getAnnotation(NextMoveBusinessMessage.class).value(), c));
    }

    public static HashBiMap<String, Class<?>> getBusinessMessages() {
        return businessMessages;
    }

    public static Set<String> getMessageTypes() {
        return businessMessages.keySet();
    }

}
