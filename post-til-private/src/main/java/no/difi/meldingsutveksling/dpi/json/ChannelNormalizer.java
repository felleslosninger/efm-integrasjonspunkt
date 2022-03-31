package no.difi.meldingsutveksling.dpi.json;

public class ChannelNormalizer {

    String normaiize(String mpcId) {
        return mpcId != null ? mpcId.replace('.', '_') : null;
    }
}
