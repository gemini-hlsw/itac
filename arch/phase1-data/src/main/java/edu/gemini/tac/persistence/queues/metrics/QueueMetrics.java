//
// $
//

package edu.gemini.tac.persistence.queues.metrics;

import edu.gemini.tac.persistence.Band;

/**
 * Performance metrics for a queue.
 */
public final class QueueMetrics {

    private final CategorizedTime total;
    private final CategorizedTime used;

    // calculate
    private final CategorizedTime remaining;

    public QueueMetrics(CategorizedTime total, CategorizedTime used) {
        this.total = total;
        this.used      = used;
        this.remaining = total.subtract(used);
    }

    /**
     * Total available queue time.
     */
    public CategorizedTime total() { return total; }

    /**
     * Used queue time.
     */
    public CategorizedTime used() { return used; }

    /**
     * Remaining queue time.  This is time that was available but couldn't be
     * filled.
     */
    public CategorizedTime remaining() { return remaining; }

    /**
     * Overall partner fairness.
     */
    public PartnerFairness fairness() {
        return new PartnerFairness(remaining.partnerView(), total.partnerView());
    }

    /**
     * Partner fairness within a particular band.
     */
    public PartnerFairness fairness(Band b) {
        return new PartnerFairness(remaining.partnerView(b), total.partnerView(b));
    }
}
