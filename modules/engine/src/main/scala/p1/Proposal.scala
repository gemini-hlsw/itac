// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.p1

import edu.gemini.tac.qengine.util.Time
import scala.annotation.tailrec
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.Site
import edu.gemini.model.p1.immutable.SpecialProposalClass
import edu.gemini.model.p1.immutable.ExchangeProposalClass
import edu.gemini.model.p1.immutable.LargeProgramClass
import edu.gemini.model.p1.immutable.SubaruIntensiveProgramClass
import edu.gemini.model.p1.immutable.FastTurnaroundProgramClass
import edu.gemini.model.p1.immutable.ClassicalProposalClass
import edu.gemini.model.p1.immutable.QueueProposalClass
import java.io.File

/**
 * The Proposal trait defines the information associated with a proposal for the purpose
 * of generating the proposal queue.
 */
sealed trait Proposal {
  def ntac: Ntac
  def site: Site
  def mode: Mode
  def too: Too.Value
  def obsList: List[Observation]
  def band3Observations: List[Observation]
  def isPoorWeather: Boolean
  def piName: Option[String]

  def jointId: Option[String] = None

  lazy val id: Proposal.Id = Proposal.Id(ntac.partner, ntac.reference)

  def obsListFor(band: QueueBand): List[Observation] =
    if (band == QueueBand.QBand3) band3Observations else obsList

  def sumObsTime(band: QueueBand): Time = Observation.sumObsTime(obsListFor(band))

  /**
   * Gets the "Core" proposal associated with the proposal.  Joint proposals
   * delegate to a core proposal. In the case of a normal core proposal,
   * this method returns <code>this</code>.
   */
  def core: CoreProposal

  /**
   * Gets the time for the proposal as a whole.
   */
  def time: Time = ntac.awardedTime

  /**
   * Returns the original awarded time for this proposal, which will be more than `awardedTime` if
   * the original proposal was split in half due to the presence of observations at both sites.
   */
  def undividedTime: Time = ntac.undividedTime.getOrElse(time)

  /**
   * Gets the time for the given observation relative to the total for all
   * observations in the proposal.
   */
  def relativeObsTime(obs: Observation, band: QueueBand): Time =
    Observation.relativeObsTime(obs, time, obsListFor(band))

  /**
   * Gets the observation list with their times adjusted to be relative to
   * the total for all observations in the proposal.
   */
  def relativeObsList(band: QueueBand): List[Observation] =
    Observation.relativeObsList(time, obsListFor(band))

  /**
   * Determines whether the given proposal has the same id, or contains a
   * proposal that contains the same id as 'that'.
   */
  def containsId(that: Proposal.Id): Boolean = id == that


  // these aren't required by the engine but are needed for generating emails
  // easiest solution is to just add them here
  def isJointComponent: Boolean = false
  def piEmail: Option[String]

  def p1proposal: edu.gemini.model.p1.immutable.Proposal
  def p1mutableProposal: edu.gemini.model.p1.mutable.Proposal

  def p1xmlFile: File

  def p1pdfFile: String =
    // p1xmlFile can be null because we need to retrofit old tests
    Option(p1xmlFile).map { f =>
      val name     = f.getName
      val baseName =
        name.lastIndexOf('.') match {
          case -1 => name
          case  n => name.substring(0, n)
        }
      s"$baseName.pdf"
    }.orNull

}

/**
 * The CoreProposal contains all the information about a proposal except for that dealing
 * with joint proposals.  Joint proposal parts and joint proposals contain one or more
 * core proposals.
 */
case class CoreProposal(
  ntac: Ntac,
  site: Site,
  mode: Mode = Mode.Queue,
  too: Too.Value = Too.none,
  obsList: List[Observation] = Nil,
  band3Observations: List[Observation] = Nil,
  isPoorWeather: Boolean = false,
  piName: Option[String] = None,
  piEmail: Option[String] = None,
  p1proposal: edu.gemini.model.p1.immutable.Proposal = null, // to avoid having to generate one for testcases that don't care
  p1mutableProposal: edu.gemini.model.p1.mutable.Proposal = null, // to avoid having to generate one for testcases that don't care
  p1xmlFile: File = null, // to avoid having to generate one for testcases that don't care
) extends Proposal {
  def core: CoreProposal = this
}

/**
 * Base class for Proposal implementations that delegate the implementation of
 * the abstract Proposal functions to another Proposal instance.
 */
sealed abstract class DelegatingProposal(coreProposal: Proposal) extends Proposal {
  def ntac: Ntac                           = coreProposal.ntac
  def site: Site                           = coreProposal.site
  def mode: Mode                           = coreProposal.mode
  def too: Too.Value                       = coreProposal.too
  def obsList: List[Observation]           = coreProposal.obsList
  def band3Observations: List[Observation] = coreProposal.band3Observations
  def isPoorWeather: Boolean               = coreProposal.isPoorWeather
  def piName: Option[String]               = coreProposal.piName
  def piEmail: Option[String]              = coreProposal.piEmail
}

/**
 * A JointProposalPart is an association of a joint proposal id and a
 * CoreProposal that represents the individual part.
 */
case class JointProposalPart(
  jointIdValue: String,
  core: CoreProposal
) extends DelegatingProposal(core) {
  override def jointId = Some(jointIdValue)
  override def isJointComponent: Boolean = true

  /**
   * Tests whether the given JointProposalPart is mergeable with this one
   * by comparing their joint ids.
   */
  def isMergeableWith(that: JointProposalPart): Boolean =
    jointIdValue == that.jointIdValue

  /**
   * Creates a JointProposal containing only this part.
   */
  def toJoint: JointProposal = JointProposal(jointIdValue, core, List(core.ntac))

  def p1proposal = core.p1proposal
  def p1mutableProposal: edu.gemini.model.p1.mutable.Proposal = core.p1mutableProposal
  def p1xmlFile = core.p1xmlFile

}

object JointProposalPart {
  def isMergeable(parts: Iterable[JointProposalPart]): Boolean =
    parts.nonEmpty && parts.tail.forall(_.isMergeableWith(parts.head))
}

/**
 * A JointProposal is a collection of proposals sharing the same joint id,
 * observations, resources, etc.  Each has its own Ntac information.
 */
case class JointProposal(jointIdValue: String, core: CoreProposal, ntacs: List[Ntac])
    extends DelegatingProposal(core) {

  // Joint proposal's "NTAC" information doesn't really make sense, except for
  // combining the awarded times.  Using the master for everything except the
  // time and the id which must be unique.
  override val ntac =
    Ntac(core.ntac.partner, jointIdValue, core.ntac.ranking, Ntac.awardedTimeSum(ntacs), false, None, None, null) // there is no submission for this

  override def jointId = Some(jointIdValue)

  /**
   * Extracts JointProposalParts that make up this JointProposal.
   */
  def toParts: List[JointProposalPart] =
    ntacs.map(n => JointProposalPart(jointIdValue, core.copy(ntac = n)))

  override def containsId(that: Proposal.Id) =
    super.containsId(that) ||
      ntacs.exists(n => (that.partner == n.partner) && (that.reference == n.reference))

  // This needs to return a p1Proposal that contains all the ntacs.
  lazy val p1proposal: edu.gemini.model.p1.immutable.Proposal = {

    // Get our old proposal and replace its proposal class with a new one with all the filled-in
    // submissions, which are hanging off the Ntac values.
    val p1proposal = core.p1proposal
    def newSubs    = ntacs.map(_.ngoSubmission)
    val newPC      =
      core.p1proposal.proposalClass match {

        // These cases can be joint.
        case pc @ ExchangeProposalClass(_, _, _, _, _)                  => pc.copy(subs = newSubs)
        case pc @ ClassicalProposalClass(_, _, _, subs, _)              => pc.copy(subs = subs.left.map(_ => newSubs))
        case pc @ QueueProposalClass(_, _, _, subs, _, _)               => pc.copy(subs = subs.left.map(_ => newSubs))

        // These can't. Go ahead and list them all out so we'll get a warning if a new class shows up.
        case pc @ SpecialProposalClass(_, _, _, _)                      => pc
        case pc @ LargeProgramClass(_, _, _, _, _)                      => pc
        case pc @ SubaruIntensiveProgramClass(_, _, _, _, _)            => pc
        case pc @ FastTurnaroundProgramClass(_, _, _, _, _, _, _, _, _) => pc

      }

    // Done!
    p1proposal.copy(proposalClass = newPC)

  }

  def p1mutableProposal: edu.gemini.model.p1.mutable.Proposal = sys.error("should never get here, this class is to be removed")

  def p1xmlFile: File = sys.error("should never get here, this class is to be removed")

}

object JointProposal {

  def apply(jointId: String, master: CoreProposal, ntacs: Ntac*): JointProposal =
    apply(jointId, master, ntacs.toList)

  /**
   * Merges the given collection of parts, returning a Some(JointProposal), if
   * there is at least one part in the collection and if all of the parts are
   * mergeable.  Returns None otherwise.
   */
  def mergeOption(parts: Iterable[JointProposalPart]): Option[JointProposal] =
    if (JointProposalPart.isMergeable(parts))
      Some(doMerge(parts))
    else
      None

  /**
   * Merges all the given parts into a single JointProposal. There must be
   * at least one part in the collection and all of the parts must be
   * mergeable.  Use mergeOption if this is not known beforehand.
   */
  def merge(parts: Iterable[JointProposalPart]): JointProposal =
    mergeOption(parts).get

  private def doMerge(parts: Iterable[JointProposalPart]): JointProposal = {
    val commonJointId = parts.head.jointIdValue
    val core          = parts.head.core
    val ntacs         = parts.toList.map(_.ntac)
    JointProposal(commonJointId, core, ntacs)
  }

  /**
   * Collects any matching parts into JointProposals.
   */
  def mergeMatching(parts: Iterable[JointProposalPart]): List[JointProposal] =
    parts.toList.groupBy(_.jointIdValue).toList.map {
      case (_, mergeableParts) => doMerge(mergeableParts)
    }
}

object Proposal {
  final case class Id(partner: Partner, reference: String) extends Ordered[Id] {
    def compare(that: Id) = partner.id.compare(that.partner.id) match {
      case 0 => reference.compare(that.reference)
      case n => n
    }
    override def toString: String = "%s(%s)".format(partner, reference)
  }

  /**
   *  An ordering based upon awarded time (descending) followed by partner
   * percentage (ascending).
   */
  object MasterOrdering extends Ordering[Proposal] {
    def compare(x: Proposal, y: Proposal): Int =
      Ntac.MasterOrdering.compare(x.ntac, y.ntac)
  }

  @tailrec
  private def expandJoints(rem: List[Proposal], out: List[Proposal]): List[Proposal] =
    rem match {
      case Nil                           => out.reverse
      case (head: JointProposal) :: tail => expandJoints(tail, head.toParts.reverse ::: out)
      case head :: tail                  => expandJoints(tail, head :: out)
    }

  /**
   * Expands any JointProposal in the given propList into its constituent parts.
   * The result list will contain only NonJointProposal and JointProposalPart.
   * Preserves the order of the input list, including the order of any joint
   * proposal parts within JointProposals.
   */
  def expandJoints(propList: List[Proposal]): List[Proposal] =
    expandJoints(propList, Nil)

  /**
   * Sums the awarded time for the proposals in the given
   */
  def sumTimes(lst: Traversable[Proposal]): Time = lst.foldLeft(Time.ZeroHours)(_ + _.time)
}
