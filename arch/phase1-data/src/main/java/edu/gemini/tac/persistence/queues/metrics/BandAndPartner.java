//
// $
//

package edu.gemini.tac.persistence.queues.metrics;

import edu.gemini.tac.persistence.Band;
import edu.gemini.tac.persistence.Partner;

// In scala, all of this would just be:
//     case class BandAndPartner(band: Band, partner: Partner)
// or even
//     (Band, Partner)

/**
 * Keys the map that categorizes time according to band and partner.
 */
public final class BandAndPartner {
    private final Band band;
    private final Partner partner;

    public BandAndPartner(Band band, Partner partner) {
        if ((band == null) || (partner == null)) {
            throw new IllegalArgumentException();
        }
        this.band    = band;
        this.partner = partner;
    }

    public Band band() {
        return band;
    }

    public Partner partner() {
        return partner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BandAndPartner that = (BandAndPartner) o;

        if (band != that.band) return false;
        if (!partner.equals(that.partner)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = band.hashCode();
        result = 31 * result + partner.hashCode();
        return result;
    }
}
