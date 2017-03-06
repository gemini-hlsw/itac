package edu.gemini.tac.psconversion

import edu.gemini.tac.persistence.{Partner => PsPartner, Site => PsSite}
import edu.gemini.tac.persistence.phase1.queues.{PartnerPercentage => PsPartnerPercentage}
import edu.gemini.tac.persistence.queues.Queue
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.util.Percent
import edu.gemini.tac.service.HibernateSource

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._


/**
 * Partners exists to encapsulate the set of Partners that are produced by the Configuration
 */

class Partners(val values : List[Partner], val psPartnerFor : Map[Partner,  PsPartner]) {
  def parse(s : String) : Option[Partner] = values.find(_.id.equals(s))

  /**
   * Creates a map with entries for all Partners according to the value
   * returned by the supplied function.
   */
  def mkMap[T](f: Partner => T): Map[Partner, T] =
    values.map(p => p -> f(p)).toMap

  // Completes the given PartialFunction by returning a default value for any
  // Partner for which pf is not defined.
  private def complete[T](pf: PartialFunction[Partner, T], default: T): Partner => T =
    p => pf.lift(p).getOrElse(default)

  /**
   * Creates a map with entries for all Partners according to the value
   * returned by the supplied partial function, using the supplied default
   * for any values not in pf's domain.
   */
  def mkMap[T](pf: PartialFunction[Partner, T], default: T): Map[Partner, T] =
    mkMap(complete(pf, default))

  def find(psPartner : PsPartner) : Option[Partner] = psPartnerFor.keys.find(psPartnerFor(_) == psPartner)
}


object Partners{
  private val Epsilon = 0.1

  // Roughly they should add up to 100 or we will complain.
  def validatePercentages(percs: List[Double]): PsError \/ Unit =
    if ((percs.sum - 100.0).abs < Epsilon) ().right
    else BadData("Partner percentages don't sum to 100.").left

  def partnerPair(ps: PsPartner, perc: Percent): PsError \/ (Partner, PsPartner) =
    for {
      p <- PartnerConverter.read(ps)
    } yield (p.copy(share = perc), ps)

  def partnerPairs(ps: List[PsPartner], percs: List[Percent]): PsError \/ List[(Partner, PsPartner)] =
    ps.zip(percs).map {
      case (psPartner, perc) => partnerPair(psPartner, perc)
    }.sequenceU

  def fromDatabase(site: PsSite): PsError \/ Partners = {
    def percentages(site: PsSite, ps: PsPartner): PsError \/ Double =
      for {
        proportion <- nullSafePersistence("Partner missing partner percentage") { ps.getPercentageShare(site) }
      } yield proportion * 100.0

    for {
      psPartners <- nullSafePersistence("No partners specified in the database") { HibernateSource.partners }
      rawPercs   <- psPartners.map(p => percentages(site, p)).sequenceU
      _          <- validatePercentages(rawPercs)
      percs = Percent.relativePercentages(rawPercs)
      res        <- partnerPairs(psPartners, percs)
    } yield new Partners(res.unzip._1, res.toMap)
  }

  def fromQueue(queue : Queue) : PsError \/ Partners = {
    def partnerAndPercentage(site: PsSite, pp: PsPartnerPercentage): PsError \/ (PsPartner, Double) =
      for {
        psPartner  <- nullSafePersistence("Partner missing in partner percentage") { pp.getPartner }
        proportion <- nullSafePersistence("Missing partner percentage") { pp.getPercentage }
        sites      <- nullSafePersistence("Partner missing sites") { psPartner.getSites }
        multiplier = if (sites.asScala.map(_.getDisplayName).contains(site.getDisplayName)) 100.0 else 0.0
      } yield (psPartner, proportion * multiplier)

    def partnerPair(ps: PsPartner, perc: Percent): PsError \/ (Partner, PsPartner) =
      for {
        p <- PartnerConverter.read(ps)
      } yield (p.copy(share = perc), ps)

    def partnerPairs(ps: List[PsPartner], percs: List[Percent]): PsError \/ List[(Partner, PsPartner)] =
      ps.zip(percs).map {
        case (psPartner, perc) => partnerPair(psPartner, perc)
      }.sequenceU

    for {
      pp    <- nullSafePersistence("Partner percentages not set") { queue.getPartnerPercentages }
      site  <- nullSafePersistence("Queue contains no site") { queue.getSite }
      _     <- SiteConverter.read(site)
      pairs <- pp.asScala.toList.map(p => partnerAndPercentage(site, p)).sequenceU
      _     <- validatePercentages(pairs.unzip._2)
      percs = Percent.relativePercentages(pairs.unzip._2)
      res   <- partnerPairs(pairs.unzip._1, percs)
    } yield new Partners(res.unzip._1, res.toMap)
  }
}