// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.p2.rollover.RolloverReport

/**
 * A combination of configuration required by the Queue Engine.
 */
final case class QueueEngineConfig(
  binConfig:           SiteSemesterConfig,
  partnerSeq:          PartnerSequence,
  rollover:            RolloverReport,
  restrictedBinConfig: RestrictionConfig = RestrictionConfig(),
) {
  def site = binConfig.site
}
