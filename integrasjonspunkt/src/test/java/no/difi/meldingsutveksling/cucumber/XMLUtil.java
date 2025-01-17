package no.difi.meldingsutveksling.cucumber;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

@UtilityClass
class XMLUtil {

    private static final String NS_ALL = "*";

    private static String getPath(Node in) {
        List<String> names = new ArrayList<>();

        for (Node node = in; node.getParentNode() != null; node = node.getParentNode()) {
            names.add(node.getNodeName());
        }

        ListIterator li = names.listIterator(names.size());

        StringBuilder sb = new StringBuilder();

        while (li.hasPrevious()) {
            sb.append(li.previous());
            if (li.hasPrevious()) {
                sb.append("/");
            }
        }

        return sb.toString();
    }

    static Element getElementByTagPath(Element in, String... localNames) {
        Element out = in;

        for (String localName : localNames) {
            out = getElementByTagName(out, localName);
        }

        return out;
    }

    private static Element getElementByTagName(Element in, String localName) {
        return getElementByTagNameNS(in, NS_ALL, localName);
    }

    private static Element getElementByTagNameNS(Element in, String namespaceURI,
                                                 String localName) {
        NodeList sigNode = in.getElementsByTagNameNS(namespaceURI, localName);

        if (sigNode.getLength() == 1) {
            return (Element) sigNode.item(0);
        }

        throw new MeldingsUtvekslingRuntimeException("%d %s children of %s".formatted(sigNode.getLength(), localName, getPath(in)));
    }
}
