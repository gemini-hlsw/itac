package edu.gemini.tac.qengine.impl.resource

import edu.gemini.tac.qengine.api.config.ConditionsCategory.{Le, Ge}
import edu.gemini.tac.qengine.util.{BoundedTime, Percent, Time}
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.p1.CloudCover._
import edu.gemini.tac.qengine.p1.SkyBackground._
import edu.gemini.tac.qengine.p1.ImageQuality._
import edu.gemini.tac.qengine.p1.WaterVapor._
import edu.gemini.tac.qengine.api.config._
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder
import edu.gemini.tac.qengine.api.queue.time.{PartnerTime, QueueTime}
import edu.gemini.tac.qengine.ctx.{TestPartners}
import edu.gemini.spModel.core.{Semester, Site}

object Fixture {
  val site = Site.GS
  val semester = new Semester(2011, Semester.Half.A)
  val partners = TestPartners.All

  // (-90,  0]   0%
  // (  0, 45] 100%
  // ( 45, 90)  50%
  val decBins   = DecBinGroup.fromBins(
    DecBin( 0, 45, Percent(100)),
    DecBin(45, 90, Percent( 50)).inclusive
  )

  // <=CC70 50%
  // >=CC80 50%
  val condsBins = ConditionsBinGroup.ofPercent(
    (ConditionsCategory(Le(CC70)), 50), (ConditionsCategory(Ge(CC80)), 50)
  )

  // 0 hrs, 1 hrs, 2 hrs, ... 23 hrs
  val raLimits   = RaBinGroup((0 to 23).map(Time.hours(_)))
  val binConfig  = new SiteSemesterConfig(site, semester, raLimits, decBins, List.empty, condsBins)
  val raResGroup = RaResourceGroup(binConfig)

  def timeResourceGroup(total: Time): TimeResourceGroup = {
    val bins = RestrictionConfig().mapTimeRestrictions(
      perc => BoundedTime(total * perc),
      time => BoundedTime(total))
    new TimeResourceGroup(bins.map(new TimeResource(_)))
  }

  def bandResource = new BandResource(RestrictionConfig().bandRestrictions)

  def semesterRes(total: Time): SemesterResource =
    new SemesterResource(raResGroup, timeResourceGroup(total), bandResource)

  // Falls in the first conditions bin (<=CC70)
  val goodCC = ObservingConditions(CC50, IQAny, SBAny, WVAny)

  // Falls in the second conditions bin (>=CC80)
  val badCC  = ObservingConditions(CC80, IQAny, SBAny, WVAny)

  def genQuanta(hrs: Double): PartnerTime = PartnerTime.constant(Time.hours(hrs), partners)

  // Makes a proposal with the given ntac info, and observations according
  // to the descriptions (target, conditions, time)
  def mkProp(ntac: Ntac, obsDefs: (Target, ObservingConditions, Time)*): CoreProposal =
    CoreProposal(ntac, site = site, obsList = obsDefs.map(tup => Observation(tup._1, tup._2, tup._3)).toList)

  val emptyQueue = ProposalQueueBuilder(QueueTime(Site.GN, PartnerTime.empty(partners).map, partners))
  def evenQueue(hrs: Double): ProposalQueueBuilder =
    evenQueue(hrs, Some(QueueTime.DefaultPartnerOverfillAllowance))

  def evenQueue(hrs: Double, overfill: Option[Percent]): ProposalQueueBuilder = {
    val pt = PartnerTime(partners, partners.map(p => (p, Time.hours(hrs))): _*)
    ProposalQueueBuilder(QueueTime(site, pt, QueueBandPercentages(), overfill))
  }
}