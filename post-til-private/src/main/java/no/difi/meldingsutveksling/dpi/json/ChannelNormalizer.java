package no.difi.meldingsutveksling.dpi.json;

public class ChannelNormalizer {

    String normalize(String mpcId) {
        return mpcId != null ? mpcId.replace('.', '_') : null;
    }

}
