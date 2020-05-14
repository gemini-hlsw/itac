// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
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
import com.itextpdf.text.log.LoggerFactory

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

class ProposalIo(partners: Map[String, Partner]) {
  val ntacIo = new NtacIo(partners)

  /**
    * Extracts Queue Engine proposal information from the immutable proposal.
    * May return multiple queue engine proposals since they must be split by
    * site.
    */
  def read(
      p: im.Proposal,
      when: Long,
      jointIdGen: JointIdGen
  ): ValidationNel[String, (NonEmptyList[Proposal], JointIdGen)] = {

    def read(
        obsGroups: ObservationIo.GroupedObservations
    )(ntacs: NonEmptyList[Ntac]): (NonEmptyList[Proposal], JointIdGen) = {
      // Discover which sites are used in the observations.
      val sites = obsGroups.map(_._1).list.toList.distinct

      // Get the total estimated time at a given site.
      def totalEstimatedTime(site: Site) =
        obsGroups.list.collect {
          case (`site`, _, os) => os.foldMap(_.time)
        } .foldMap(identity)

      // Get estimated time for each site, as well as their sum.
      val tetGN = totalEstimatedTime(Site.GN)
      val tetGS = totalEstimatedTime(Site.GS)
      val tet   = tetGN + tetGS

      // Make a proposal per site represented in the observations.
      val (props, newGen) = sites.foldLeft((List.empty[Proposal], jointIdGen)) {
        case ((propList, gen), site) =>

          // Get the list of observations associated with the given band category (if any).
          def bandList(cat: QueueBand.Category): List[Observation] =
            ~obsGroups.list
              .find { case (s, b, _) => s == site && b == cat }
              .map(_._3.list.toList)

          // The first NTAC is the one we care about here (why?)
          val ntac = ntacs.head

          // Figure out the proportion of estimated time we're using for the current site, and
          // scale the total award by that amount. For proposals that are only at one site the
          // proportion is 100% and the scaling is a no-op.
          val tetThis = if (site == Site.GN) tetGN else tetGS
          val proportion = tetThis.ms.toDouble / tet.ms.toDouble
          val scaledAward = ntac.awardedTime * Percent(proportion * 100)
          val ntacʹ = ntac.copy(awardedTime = scaledAward)

          if (proportion != 1.0)
            println(f"${ntac.reference}%-15s est for ${site.abbreviation} is ${tetThis.toHours.value}%5.1f h (${proportion * 100}%5.1f%% of total) ... scaled award is ${scaledAward.toHours.value}%5.1f of ${ntac.awardedTime.toHours.value}%5.1f")

          // Make the corresponding CoreProposal
          val b12 = bandList(QueueBand.Category.B1_2)
          val b3 = bandList(QueueBand.Category.B3)
          val core = CoreProposal(
            ntacʹ,
            site,
            mode(p),
            too(p),
            b12,
            b3,
            ntac.poorWeather,
            piName(p),
            piEmail(p),
            p
          )

          // If there are more ntacs, it is a Joint, otherwise just this core.
          val (prop, newGen) = ntacs.tail.toList match {
            case Nil => (core, gen)
            case _ =>
              (JointProposal(gen.toString, core, ntacs.list.toList), gen.next)
          }
          (prop :: propList, newGen)
      }

      // GroupedObservations is a NonEmptyList so props cannot be Nil
      (NonEmptyList(props.head, props.tail: _*), newGen)
    }

    ntacIo.read(p.copy(observations = p.observations.filter(_.enabled))) <*> ObservationIo
      .readAllAndGroup(p, when)
      .map(read)
  }

}
