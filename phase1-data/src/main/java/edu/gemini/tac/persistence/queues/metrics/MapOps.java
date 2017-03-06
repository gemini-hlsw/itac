//
// $
//

package edu.gemini.tac.persistence.queues.metrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Factors out repetitive math operations needed for computing the queue
 * metrics.
 */
final class MapOps {
    protected MapOps() {}

    interface Op {
        double apply(double v1, double v2);
    }

    static final Op ADD = new Op() {
        public double apply(double v1, double v2) { return v1 + v2; }
    };

    static final Op SUBTRACT = new Op() {
        public double apply(double v1, double v2) { return v1 - v2; }
    };

    static final Op POW = new Op() {
        public double apply(double v1, double v2) { return Math.pow(v1, v2); }
    };

    static final Op PERCENT = new Op() {
        public double apply(double v1, double v2) {
            return (v2 == 0) ? 0.0 : v1/v2 * 100;
        }
    };

    /**
     * Combined keys from the two maps.
     */
    static <K> Set<K> keySet(Map<K, Double> m1, Map<K, Double> m2) {
        Set<K> res = new HashSet<K>(m1.keySet());
        res.addAll(m2.keySet());
        return res;
    }

    /**
     * Unboxes the Double object, mapping null to 0.0.
     */
    static double unbox(Double d) { return (d == null) ? 0.0 : d; }

    /**
     * Combines two maps by applying the given op to every matching value.
     */
    static <K> Map<K, Double> combine(Map<K, Double> m1, Op op, Map<K, Double> m2) {
        Map<K, Double> res = new HashMap<K, Double>();
        for (K k : keySet(m1, m2)) {
            double v1 = unbox(m1.get(k));
            double v2 = unbox(m2.get(k));
            res.put(k, op.apply(v1, v2));
        }
        return res;
    }

    /**
     * Apples the given op on every entry in the map using the given constant
     * value for all entries.
     */
    static <K> Map<K, Double> apply(Map<K, Double> m, Op op, double value) {
        Map<K, Double> res = new HashMap<K, Double>();
        for (K k : m.keySet()) {
            double t = unbox(m.get(k));
            res.put(k, op.apply(t, value));
        }
        return res;
    }

    /**
     * Starting with initialValue, applies the given op to every value of the
     * map, passing in the result of the previous calculation.
     */
    static <K> double fold(double initialValue, Op op, Map<K, Double> m) {
        double val = initialValue;
        for (K k : m.keySet()) val = op.apply(unbox(m.get(k)), val);
        return val;
    }

    /**
     * Sums the values in the map.
     */
    static <K> double sum(Map<K, Double> m) { return fold(0.0, ADD, m); }

    /**
     * Computes the average value in the map.
     */
    static <K> double avg(Map<K, Double> m) { return sum(m) / m.keySet().size(); }
}
