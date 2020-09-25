// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p2.rollover.RolloverReport
import edu.gemini.tac.qengine.p1.QueueBand

/**
 * A combination of configuration required by the Queue Engine.
 */
final case class QueueEngineConfig(
  partners: List[Partner],
  binConfig: SiteSemesterConfig,
  partnerSeq: PartnerSequence,
  rollover: RolloverReport,
  restrictedBinConfig: RestrictionConfig = RestrictionConfig(),
  explicitQueueAssignments: Map[String, QueueBand] = Map.empty
) {
  def site = binConfig.site
}
