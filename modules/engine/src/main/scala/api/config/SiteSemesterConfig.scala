// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.util.{Percent, Time}
import edu.gemini.spModel.core.{Semester, Site}
import edu.gemini.tac.qengine.ctx.Context

// TODO:
// The RaBinGroup[Time] limits and DecBinGroup[Percent] can be calculated given
// the site and semester.  The conditions may differ according to site and
// semester so there should probably 4 defaults.

/**
 * This class represents a grouping of configuration information that will
 * differ according to telescope site and semester.  These items will be used
 * to track time for observation targets and site conditions and ensure that
 * the engine stays under budget.
 */
final class SiteSemesterConfig(
        val site: Site,
        val semester: Semester,
        val raLimits: RaBinGroup[Time],
        val decLimits: DecBinGroup[Percent],
        val shutdowns : List[Shutdown],
        val conditions: ConditionsBinGroup[Percent] = Default.Conditions) {

  // At least one DecBin has to be allocated 100%.  If all are less than 100%,
  // the logic for creating ToO blocks is flawed.  It only checks the
  // remaining time for the observation's obs conditions, relying on the fact
  // that if there is sufficient remaining time in the RA bin overall, there
  // must be remaining time in at least one dec bin.
  require(decLimits.bins.exists(_.binValue.value == 100))

  def context: Context = new Context(site, semester)
}