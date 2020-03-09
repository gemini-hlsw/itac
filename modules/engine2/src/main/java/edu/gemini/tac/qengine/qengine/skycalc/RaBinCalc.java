//
// $
//

package edu.gemini.qengine.skycalc;

import edu.gemini.tac.qengine.ctx.Semester;
import edu.gemini.tac.qengine.ctx.Site;

import java.util.Date;
import java.util.List;

/**
 * Describes the contract for calculating the RaBin hour values.  There are
 * different methods for computing this value.
 */
public interface RaBinCalc {
    List<Hours> calc(Site site, Date start, Date end, RaBinSize size);
}
