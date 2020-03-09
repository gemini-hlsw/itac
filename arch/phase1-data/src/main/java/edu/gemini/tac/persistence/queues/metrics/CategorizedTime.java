//
// $
//

package edu.gemini.tac.persistence.queues.metrics;

import edu.gemini.tac.persistence.Band;
import edu.gemini.tac.persistence.Partner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Time information including an overall total, a total per partner, a total
 * per queue band, and a total per partner and queue band.  Time is understood
 * to be an amount in hours.
 */
public final class CategorizedTime {

    // This is what we need to store in Queue
    private final Map<BandAndPartner, Double> bandAndPartnerMap;

    private final double total;                    // calculated
    private final Map<Partner, Double> partnerMap; // calculated
    private final Map<Band, Double> bandMap;       // calculated

    public CategorizedTime(Map<BandAndPartner, Double> bandAndPartnerMap) {
        this.total      = MapOps.sum(bandAndPartnerMap);
        this.partnerMap = groupBy(PARTNER_EXTRACTOR, bandAndPartnerMap);
        this.bandMap    = groupBy(BAND_EXTRACTOR, bandAndPartnerMap);
        this.bandAndPartnerMap = Collections.unmodifiableMap(new HashMap<BandAndPartner, Double>(bandAndPartnerMap));
    }

    private interface GroupExtractor<G> { G extract(BandAndPartner bp); }
    private static final GroupExtractor<Band> BAND_EXTRACTOR = new GroupExtractor<Band>() {
        public Band extract(BandAndPartner bp) { return bp.band(); }
    };
    private static final GroupExtractor<Partner> PARTNER_EXTRACTOR = new GroupExtractor<Partner>() {
        public Partner extract(BandAndPartner bp) { return bp.partner(); }
    };

    private static <K> Map<K, Double> groupBy(GroupExtractor<K> ex, Map<BandAndPartner, Double> m) {
        Map<K, Double> res = new HashMap<K, Double>();
        for (BandAndPartner bp : m.keySet()) {
            K      key = ex.extract(bp);
            Double val = MapOps.unbox(m.get(bp));
            Double cur = MapOps.unbox(res.get(key));
            res.put(key, cur + val);
        }
        return Collections.unmodifiableMap(res);
    }

    /**
     * Total time.
     */
    public double total() {
        return total;
    }

    public Set<Partner> partners() {
        return partnerMap.keySet();
    }

    public double forPartner(Partner partner) {
        return MapOps.unbox(partnerMap.get(partner));
    }

    /**
     * Time for specific partners across all bands.
     */
    public Map<Partner, Double> partnerView() {
        return partnerMap;
    }

    /**
     * Time for specific partners in the given queue band.
     */
    public Map<Partner, Double> partnerView(Band band) {
        Map<Partner, Double> m = new HashMap<Partner, Double>();
        for (Partner p : partners()) m.put(p, forBandAndPartner(band, p));
        return m;
    }

    public Set<Band> bands() {
        return bandMap.keySet();
    }

    public double forBand(Band band) {
        return MapOps.unbox(bandMap.get(band));
    }

    /**
     * Time for specific bands across all partners.
     */
    public Map<Band, Double> bandView() {
        return bandMap;
    }

    /**
     * Time for specific bands for the given partner.
     */
    public Map<Band, Double> bandView(Partner partner) {
        Map<Band, Double> m = new HashMap<Band, Double>();
        for (Band b : bands()) m.put(b, forBandAndPartner(b, partner));
        return m;
    }

    public Set<BandAndPartner> bandAndPartners() {
        return bandAndPartnerMap.keySet();
    }

    public double forBandAndPartner(Band band, Partner partner) {
        return MapOps.unbox(bandAndPartnerMap.get(new BandAndPartner(band, partner)));
    }

    /**
     * Time for specific bands and partners.
     */
    public Map<BandAndPartner, Double> bandAndPartnerView() {
        return bandAndPartnerMap;
    }

    private CategorizedTime applyOp(MapOps.Op op, CategorizedTime that) {
        Map<BandAndPartner, Double> bp;
        bp = MapOps.combine(bandAndPartnerMap, op, that.bandAndPartnerMap);
        return new CategorizedTime(bp);
    }

    public CategorizedTime add(CategorizedTime that) {
        return applyOp(MapOps.ADD, that);
    }

    public CategorizedTime subtract(CategorizedTime that) {
        return applyOp(MapOps.SUBTRACT, that);
    }
}
