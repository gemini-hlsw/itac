package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p2.rollover.RolloverReport

object QueueEngineConfig {

  /**
   * Creates a QueueEngineConfig using the given site and semester configuration
   * but defaulting the partner sequence and restriction configuration.
   */
  def apply(partners: List[Partner], binConfig: SiteSemesterConfig, partnerSequence : PartnerSequence): QueueEngineConfig =
    new QueueEngineConfig(
      partners,
      binConfig,
      partnerSequence,
      RolloverReport.empty,
      RestrictionConfig())

  /**
   * Creates a QueueEngineConfig with the given site and semester information,
   * the given partner sequence, and an optionally specified restriction
   * configuration.
   */
  def apply(partners: List[Partner],
            binConfig: SiteSemesterConfig,
            partnerSeq: PartnerSequence,
            rollover: RolloverReport,
            restrictedBinConfig: RestrictionConfig = RestrictionConfig()): QueueEngineConfig =
    new QueueEngineConfig(partners, binConfig, partnerSeq, rollover, restrictedBinConfig)
}

/**
 * A combination of configuration required by the Queue Engine.
 */
final class QueueEngineConfig(
  val partners: List[Partner],
  val binConfig: SiteSemesterConfig,
  val partnerSeq: PartnerSequence,
  val rollover: RolloverReport,
  val restrictedBinConfig: RestrictionConfig)
