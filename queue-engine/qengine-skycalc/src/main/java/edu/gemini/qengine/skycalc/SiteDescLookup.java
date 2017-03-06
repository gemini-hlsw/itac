//
// $
//

package edu.gemini.qengine.skycalc;

import edu.gemini.shared.skycalc.SiteDesc;
import edu.gemini.tac.qengine.ctx.Site;

/**
 * A single place for finding the SiteDesc that corresponds to a Site.
 */
public final class SiteDescLookup {
    private SiteDescLookup() {}

    public static SiteDesc get(Site site) {
        return (site == Site.north) ? SiteDesc.MAUNA_KEA : SiteDesc.CERRO_PACHON;
    }
}
