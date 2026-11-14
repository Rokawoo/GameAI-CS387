package agents.firstLast.bt;

import java.util.HashMap;
import java.util.Map;

// a leaf of the tree, game specific
// gets its knobs from the xml attributes
public abstract class Leaf extends Task {
    protected Map<String, String> params = new HashMap<>();

    public void configure(Map<String, String> p) {
        params = p;
        setup();
    }

    // leaves read their params here
    protected void setup() {
    }

    protected double num(String key, double fallback) {
        String v = params.get(key);
        if (v == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
