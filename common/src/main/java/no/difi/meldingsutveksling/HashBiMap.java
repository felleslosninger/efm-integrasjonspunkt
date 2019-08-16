package no.difi.meldingsutveksling;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HashBiMap<K, V> extends HashMap<K, V> {

    private final HashMap<V, K> inverse = new HashMap<>();

    @Override
    public V put(K key, V value) {
        inverse.put(value, key);
        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            inverse.put(entry.getValue(), entry.getKey());
        }

        super.putAll(m);
    }

    public Map<V, K> inverse() {
        return Collections.unmodifiableMap(inverse);
    }
}
