package edu.gemini.tac.qengine.p1

import edu.gemini.tac.qengine.util.Time
import scala.annotation.tailrec
import edu.gemini.tac.qengine.ctx.{Partner, Site}

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
  def band3Observations : List[Observation]
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

  def toXML =
    <Proposal id={ id.toString }>
      <PI>{ piName }</PI>
    </Proposal>
}

/**
 * The CoreProposal contains all the information about a proposal except for that dealing
 * with joint proposals.  Joint proposal parts and joint proposals contain one or more
 * core proposals.
 */
case class CoreProposal(
  ntac: Ntac,
  site: Site,
  mode: Mode                 = Mode.Queue,
  too: Too.Value             = Too.none,
  obsList: List[Observation] = Nil,
  band3Observations : List[Observation] = Nil,
  isPoorWeather: Boolean     = false,
  piName: Option[String]     = None
) extends Proposal {
  def core: CoreProposal = this
}

/**
 * Base class for Proposal implementations that delegate the implementation of
 * the abstract Proposal functions to another Proposal instance.
 */
abstract class DelegatingProposal(coreProposal: Proposal) extends Proposal {
  def ntac: Ntac                            = coreProposal.ntac
  def site: Site                            = coreProposal.site
  def mode: Mode                            = coreProposal.mode
  def too: Too.Value                        = coreProposal.too
  def obsList: List[Observation]            = coreProposal.obsList
  def band3Observations : List[Observation] = coreProposal.band3Observations
  def isPoorWeather: Boolean                = coreProposal.isPoorWeather
  def piName: Option[String]                = coreProposal.piName
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
}

object JointProposalPart {
  def isMergeable(parts: Iterable[JointProposalPart]): Boolean =
    parts.nonEmpty && parts.tail.forall(_.isMergeableWith(parts.head))
}

/**
 * A JointProposal is a collection of proposals sharing the same joint id,
 * observations, resources, etc.  Each has its own Ntac information.
 */
case class JointProposal(jointIdValue: String, core: CoreProposal, ntacs: List[Ntac]) extends DelegatingProposal(core) {

  // Joint proposal's "NTAC" information doesn't really make sense, except for
  // combining the awarded times.  Using the master for everything except the
  // time and the id which must be unique.
  override val ntac    = Ntac(core.ntac.partner, jointIdValue, core.ntac.ranking, Ntac.awardedTimeSum(ntacs))

  override def jointId = Some(jointIdValue)

  /**
   * Extracts JointProposalParts that make up this JointProposal.
   */
  def toParts: List[JointProposalPart] =
    ntacs.map(n => JointProposalPart(jointIdValue, core.copy(ntac = n)))

  override def containsId(that: Proposal.Id) =
    super.containsId(that) ||
    ntacs.exists(n => (that.partner == n.partner) && (that.reference == n.reference))
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
      case Nil => out.reverse
      case (head: JointProposal) :: tail => expandJoints(tail, head.toParts.reverse ::: out)
      case head :: tail => expandJoints(tail, head :: out)
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
  def sumTimes(lst: Traversable[Proposal]): Time = (Time.ZeroHours/:lst)(_ + _.time)
}