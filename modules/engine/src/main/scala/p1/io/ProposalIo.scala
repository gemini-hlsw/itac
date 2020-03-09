// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.p1.io

import edu.gemini.model.p1.{immutable => im}
import edu.gemini.model.p1.{mutable => m}
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.p1._

import scalaz._
import Scalaz._

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

      // Make a proposal per site represented in the observations.
      val (props, newGen) = sites.foldLeft((List.empty[Proposal], jointIdGen)) {
        case ((propList, gen), site) =>
          // Get the list of observations associated with the given band category (if any).
          def bandList(cat: QueueBand.Category): List[Observation] =
            ~obsGroups.list.find { case (s, b, _) => s == site && b == cat }.map(_._3.list.toList)

          // Make the corresponding CoreProposal
          val ntac = ntacs.head
          val b12  = bandList(QueueBand.Category.B1_2)
          val b3   = bandList(QueueBand.Category.B3)
          val core = CoreProposal(ntac, site, mode(p), too(p), b12, b3, ntac.poorWeather, piName(p), piEmail(p), Some(p))

          // If there are more ntacs, it is a Joint, otherwise just this core.
          val (prop, newGen) = ntacs.tail.toList match {
            case Nil => (core, gen)
            case _   => (JointProposal(gen.toString, core, ntacs.list.toList), gen.next)
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
