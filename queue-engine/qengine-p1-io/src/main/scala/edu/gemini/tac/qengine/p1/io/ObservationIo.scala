package edu.gemini.tac.qengine.p1.io

import edu.gemini.model.p1.{immutable => im}
import edu.gemini.model.p1.{mutable => m}
import edu.gemini.spModel.core.Coordinates
import m.CloudCover._
import m.ImageQuality._
import m.SkyBackground._
import m.WaterVapor._
import edu.gemini.tac.qengine.ctx.Site
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.util.Time

import scalaz._
import Scalaz._
import scalaz.Validation.FlatMap._

/**
 * Extracts relevant information out of a Phase 1 observation, if possible.
 */
object ObservationIo {
  def MISSING_BLUEPRINT    = "Observation missing blueprint definition"
  def MISSING_CONDITIONS   = "Observation missing observing conditions definition"
  def MISSING_OBSERVATIONS = "No observations in proposal"
  def MISSING_TARGET       = "Observation missing target definition"
  def MISSING_TIME         = "Observation missing time amount"

  type GroupedObservations = NonEmptyList[(Site, QueueBand.Category, NonEmptyList[Observation])]

  def readAllAndGroup(p: im.Proposal, when: Long): ValidationNel[String, GroupedObservations] = {
    // Get the p1 observation's site, defaulting to GN.
    def site(p1Obs: im.Observation): Site = {
      val imSite = p1Obs.blueprint.map {
        case d: im.DssiBlueprint       => d.site
        case a: im.AlopekeBlueprint    => a.site
        case t: im.TexesBlueprint      => t.site
        case p: im.PhoenixBlueprint    => p.site
        case v: im.VisitorBlueprint    => v.site
        case g: im.GeminiBlueprintBase => g.instrument.site
        case _                         => im.Site.GN
      } | im.Site.GN
      if (imSite == im.Site.GN) Site.north else Site.south
    }

    // Get the p1 observations QueueBand.Category (Band 3 or Band 1/2).
    def band(p1Obs: im.Observation): QueueBand.Category =
      if (p1Obs.band == im.Band.BAND_3) QueueBand.Category.B3 else QueueBand.Category.B1_2

    // Site and band tuple for each observation in the proposal in the order
    // the observations appear (to be zipped with the extracted queue engine
    // observation information).
    val obsCtx = p.observations.map(o => (site(o), band(o)))

    // Queue Engine observation list extracted from p1 observations.
    val qeObsList = p.observations.map(ObservationIo.read(_, when)).sequenceU

    // Group by site and band category, but error out if no observations.
    qeObsList.flatMap { ol =>
      val grp = obsCtx.zip(ol).groupBy(_._1).map {
        case ((s, b), lst) =>
          val obsList = lst.unzip._2
          (s, b, NonEmptyList(obsList.head, obsList.tail: _*))
      }.toList

      grp match {
        case Nil    => MISSING_OBSERVATIONS.failureNel[GroupedObservations]
        case h :: t => NonEmptyList(h, t: _*).successNel[String]
      }
    }
  }

  def read(o: im.Observation, when: Long): ValidationNel[String, Observation] =
    lgs(o) <*> (time(o) <*> (conditions(o) <*> (target(o, when) map (Observation.apply _).curried)))

  val tooCoords = Coordinates.zero

  private def target(o: im.Observation, when: Long): ValidationNel[String, Target] =
    o.target.map(target(_, when)).fold(MISSING_TARGET.failureNel[Target]) { _.successNel[String] }

  def target(t: im.Target, when: Long): Target = {
    val c = t.coords(when) | tooCoords
    Target(c.ra.toAngle.toDegrees, c.dec.toDegrees, ~Option(t.name))
  }

  private def conditions(o: im.Observation): ValidationNel[String, ObsConditions] = {
    o.condition.map(conditions).fold(MISSING_CONDITIONS.failureNel[ObsConditions]) { _.successNel[String] }
  }

  def conditions(c: im.Condition): ObsConditions = {
    val cc = c.cc match {
      case `cc50`  => CloudCover.CC50
      case `cc70`  => CloudCover.CC70
      case `cc80`  => CloudCover.CC80
      case `cc100` => CloudCover.CCAny
    }

    val iq = c.iq match {
      case `iq20`  => ImageQuality.IQ20
      case `iq70`  => ImageQuality.IQ70
      case `iq85`  => ImageQuality.IQ85
      case `iq100` => ImageQuality.IQAny
    }

    val sb = c.sb match {
      case `sb20`  => SkyBackground.SB20
      case `sb50`  => SkyBackground.SB50
      case `sb80`  => SkyBackground.SB80
      case `sb100` => SkyBackground.SBAny
    }

    val wv = c.wv match {
      case `wv20`  => WaterVapor.WV20
      case `wv50`  => WaterVapor.WV50
      case `wv80`  => WaterVapor.WV80
      case `wv100` => WaterVapor.WVAny
    }

    ObsConditions(cc, iq, sb, wv)
  }

  def time(o: im.Observation): ValidationNel[String, Time] =
    o.totalTime.map(_.nonNegativeQueueEngineTime("observation")) | MISSING_TIME.failureNel[Time]

  def lgs(o: im.Observation): ValidationNel[String, Boolean] =
    o.blueprint.map {
      case g: im.GeminiBlueprintBase => g.ao.toBoolean
      case _ => false
    }.fold(MISSING_BLUEPRINT.failureNel[Boolean]) { _.successNel[String] }
}
