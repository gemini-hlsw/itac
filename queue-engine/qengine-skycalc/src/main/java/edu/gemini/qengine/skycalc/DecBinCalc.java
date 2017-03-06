//
// $
//

package edu.gemini.qengine.skycalc;

import edu.gemini.tac.qengine.ctx.Semester;
import edu.gemini.tac.qengine.ctx.Site;
import edu.gemini.shared.skycalc.Angle;

import java.util.Date;
import java.util.List;

/**
 * Describes the contract for calculating the DecBin percentages.
 */
public interface DecBinCalc {

    List<Percent> calc(Site site, Date start, Date end, DecBinSize size, Angle ra);
}
