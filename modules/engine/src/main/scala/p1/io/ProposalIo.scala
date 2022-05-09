// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.p1.io

import edu.gemini.model.p1.{immutable => im}
import edu.gemini.model.p1.{mutable => m}
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p1._

import scalaz._
import Scalaz._
import edu.gemini.spModel.core.Site
import edu.gemini.tac.qengine.util.Percent
import org.slf4j.LoggerFactory
import java.io.File

/**
  * Immutable Phase1 proposal
  */
object ProposalIo {
  def mode(p: im.Proposal): Mode =
    p.proposalClass match {
      case _: im.ClassicalProposalClass => Mode.Classical
      case _: im.LargeProgramClass      => Mode.LargeProgram
      case _                            => Mode.Queue
    }

  def too(p: im.Proposal): Too.Value = {
    def fromOpt(o: m.TooOption): Too.Value = o match {
      case m.TooOption.RAPID    => Too.rapid
      case m.TooOption.STANDARD => Too.standard
      case _                    => Too.none
    }

    p.proposalClass match {
      case q: im.QueueProposalClass => fromOpt(q.tooOption)
      case l: im.LargeProgramClass  => fromOpt(l.tooOption)
      case _                        => Too.none
    }
  }

  def piName(p: im.Proposal): Option[String] =
    Option(p.investigators.pi.lastName)

  def piEmail(p: im.Proposal): Option[String] =
    Option(p.investigators.pi.email)

}

import ProposalIo._

class ProposalIo {
  val ntacIo = new NtacIo

  // We need to split up the partners based on the sites where they are active.
  val gnPartners   = Partner.all.filter(_.sites == Set(Site.GN)).toSet
  val gsPartners   = Partner.all.filter(_.sites == Set(Site.GS)).toSet
  val dualPartners = Partner.all.filter(_.sites.size == 2).toSet

  /**
    * Extracts Queue Engine proposal information from the immutable proposal.
    * May return multiple queue engine proposals since they must be split by
    * site.
    */
  def read(
      p: im.Proposal,
      mp: m.Proposal,
      when: Long,
      p1xml: File
  ): ValidationNel[String, NonEmptyList[Proposal]] = {

    def read(
        obsGroups: ObservationIo.GroupedObservations
    )(ntacs: NonEmptyList[Ntac]): NonEmptyList[Proposal]= {
      // Although a proposal might in principle have more than one NTAC with a response, the way we
      // do things now guarantees that this will never happen! And it fact we rely on it never
      // happening. So just punt here if there's more than one.
      if (ntacs.length > 1)
        sys.error(s"Got more than one NTAC! How did this happen??!?: ${ntacs.list}")

      doRead(obsGroups, ntacs.head)
    }

    def doRead(
      obsGroups: ObservationIo.GroupedObservations,
      ntac: Ntac
    ): NonEmptyList[Proposal] = {

      // Discover which sites are used in the observations.
      val sites = obsGroups.map(_._1).list.toList.distinct

      // Get the total estimated time at a given site.
      def totalEstimatedTime(site: Site) =
        obsGroups.list.collect {
          case (`site`, _, os) => os.foldMap(_.time)
        } .foldMap(identity)

      // OK LISTEN UP

      // Some proposals have observations at both sites, so we need to figure out how much awarded
      // time goes to each site. Most partner time can be divided proportionally, but some partner
      // time can be used only at one site or the other. So that's what all the code below figures
      // out. It's repetitive and has a lot of comments with small words. I want to keep it simple.

      // Total estimated time at each site. This bears little relation to the awarded time (which is
      // almost invariably substantially less) but we use their ratio to figure out how to split up
      // time that can go to either site.
      val estimatedTimeGN = totalEstimatedTime(Site.GN)
      val estimatedTimeGS = totalEstimatedTime(Site.GS)

      // NTAC awards, divided into gn, gs, and dual-site. These are mutually exclusive. The GN award
      // can be used only at GN; the GS award can be used only at GS; and the remainder can be used
      // anywhers.
      val dedicatedAwardGN = Option(ntac).filter(n => gnPartners.contains(n.partner)).foldMap(_.awardedTime)
      val dedicatedAwardGS = Option(ntac).filter(n => gsPartners.contains(n.partner)).foldMap(_.awardedTime)
      val dualSiteAward    = Option(ntac).filter(n => dualPartners.contains(n.partner)).foldMap(_.awardedTime)

      // The estimated shared time at each site, which means the estimated time at the site, less
      // dedicated time that can only be used there. We want to remove the dedicated time and then
      // split the shared time among the remainder, proprotionally. This probably isn't quite right;
      // it might make sense to scale the awards somehow since they can be much smaller than the
      // estimated time and thus won't affect the ratio very much in some cases. But for now let's
      // call it ok.
      val estimatedSharedTimeGN = estimatedTimeGN - dedicatedAwardGN
      val estimatedSharedTimeGS = estimatedTimeGS - dedicatedAwardGS
      val totalSharedTime       = estimatedSharedTimeGN + estimatedSharedTimeGS

      // The proportion of shared time that goes to each site. If there is no shared time at all
      // then it's zero. We have to catch that case or we get an ArithmeticException later on.
      val proportionOfSharedTimeGN = if (totalSharedTime.isZero) 0.0 else estimatedSharedTimeGN.ms.toDouble / totalSharedTime.ms.toDouble
      val proportionOfSharedTimeGS = if (totalSharedTime.isZero) 0.0 else estimatedSharedTimeGS.ms.toDouble / totalSharedTime.ms.toDouble

      // Now we can split the dual-site award between sites.
      val sharedAwardGN = dualSiteAward * Percent(proportionOfSharedTimeGN * 100)
      val sharedAwardGS = dualSiteAward * Percent(proportionOfSharedTimeGS * 100)

      // And add back the dedicated time.
      val totalAwardGN = dedicatedAwardGN + sharedAwardGN
      val totalAwardGS = dedicatedAwardGS + sharedAwardGS

      // And that's it. In a moment we select which of the total awards to use.
      // Fin.

      // Make a proposal per site represented in the observations.
      val props = sites.map { site =>

          // Get the list of observations associated with the given band category (if any).
          def bandList(cat: ObservationIo.BandChoice): List[Observation] =
            ~obsGroups.list
              .find { case (s, b, _) => s == site && b == cat }
              .map(_._3.list.toList)

          // Total award at this site
          val totalAwardHere = if (site == Site.GN) totalAwardGN else totalAwardGS

          // println()
          // println(ntac.reference)
          // println(s"my gn   award is ${dedicatedAwardGN.toHours}")
          // println(s"my gs   award is ${dedicatedAwardGS.toHours}")
          // println(s"my dual award is ${dualSiteAward.toHours}")
          // println(s"my total estimated time is ${tet.toHours}")
          // println(s"my total estimated time at gn is ${estimatedTimeGN.toHours}")
          // println(s"my total estimated time at gs is ${estimatedTimeGS.toHours}")
          // println(s"subtracting site-specific award, my shared time at gn is ${estimatedSharedTimeGN.toHours}")
          // println(s"subtracting site-specific award, my shared time at gs is ${estimatedSharedTimeGS.toHours}")
          // println(s"proportion of shared time at GN is ${proportionOfSharedTimeGN}")
          // println(s"proportion of shared time at GN is ${proportionOfSharedTimeGS}")
          // println(s"shared award at GN is ${sharedAwardGN.toHours}")
          // println(s"shared award at GS is ${sharedAwardGS.toHours}")
          // println(s"total award at GN is ${totalAwardGN.toHours}")
          // println(s"total award at GS is ${totalAwardGS.toHours}")
          // println(s"---")
          // println(s"my site is $site so my award here is ${totalAwardHere.toHours}")
          // println(s"---")
          // println(ntacs.map(n => f"${n.partner.id}:${n.awardedTime.toHours.value}%5.1f").list.toList.mkString(", "))

          if (totalAwardHere.isZero) {
            LoggerFactory.getLogger("edu.gemini.itac").warn(s"Proposal ${ntac.reference} has observations at $site but no awarded time is usable there. Award was ${ntac.partner.id}: ${ntac.awardedTime.toHours}.")
          }

          val ntacʹ = ntac.copy(awardedTime = totalAwardHere, undividedTime = Some(ntac.awardedTime))

          // Make the corresponding Proposal
          val b12 = bandList(ObservationIo.BandChoice.Band124)
          val b3 = bandList(ObservationIo.BandChoice.Band3)

          // Get the itacComment, if any.
          val itacComment: Option[String] =
            p.proposalClass.itac.flatMap(_.comment)

          // done
          Proposal(
            ntacʹ,
            site,
            mode(p),
            too(p),
            b12,
            b3,
            ntac.poorWeather,
            piName(p),
            piEmail(p),
            p,
            mp,
            p1xml,
            itacComment
          )

      }

      // GroupedObservations is a NonEmptyList so props cannot be Nil
      NonEmptyList(props.head, props.tail: _*)
    }

    ntacIo.read(p.copy(observations = p.observations.filter(_.enabled))) <*> ObservationIo
      .readAllAndGroup(p, when)
      .map(read)
  }

}
