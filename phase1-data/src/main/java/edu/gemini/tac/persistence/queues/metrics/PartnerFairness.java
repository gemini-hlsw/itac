//
// $
//

package edu.gemini.tac.persistence.queues.metrics;

import edu.gemini.tac.persistence.Partner;

import java.util.Map;

// There is no need to store this, since we can compute it easily.


/**
 * Partner fairness calculation.  Measured as a percentage of error from
 * the partner allocation.  Can be calculated
 *
 * Percentages are used because partners have wildly differing time allocations.
 * The standard deviation of all remaining time percentages gives an indication
 * of how fair the algorithm has been.
 */
public final class PartnerFairness {

    private final double standardDeviation;
    private final Map<Partner, Double> percentErrorMap;

    public PartnerFairness(Map<Partner, Double> remaining, Map<Partner, Double> available) {
        percentErrorMap = MapOps.combine(remaining, MapOps.PERCENT, available);
        double meanErrorPercent = MapOps.avg(percentErrorMap);

        Map<Partner, Double> diff   = MapOps.apply(percentErrorMap, MapOps.SUBTRACT, meanErrorPercent);
        Map<Partner, Double> sqDiff = MapOps.apply(diff, MapOps.POW, 2.0);
        standardDeviation = Math.sqrt(MapOps.avg(sqDiff));
    }

    public double standardDeviation() {
        return standardDeviation;
    }

    public Map<Partner, Double> percentageErrorView() {
        return percentErrorMap;
    }

    public double errorFor(Partner p) {
        return MapOps.unbox(percentErrorMap.get(p));
    }
}
