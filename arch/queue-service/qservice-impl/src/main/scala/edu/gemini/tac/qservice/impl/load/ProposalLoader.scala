package edu.gemini.tac.qservice.impl.load

import edu.gemini.model.p1.{immutable => im}
import edu.gemini.tac.qengine.ctx.{Context, Partner}
import edu.gemini.tac.qengine.p1.{JointProposal, Proposal}
import edu.gemini.tac.qengine.p1.io._
import edu.gemini.tac.{persistence => ps}
import edu.gemini.tac.psconversion._
import edu.gemini.tac.util.HibernateToMutableConverter

import scalaz._
import Scalaz._
import java.util.logging.{Level, Logger}

/**
 * This is where a persistence proposal is turned into something the queue
 * engine can work with without going directly to the database.
 */
object ProposalLoader {
  val Log = Logger.getLogger(getClass.getName)

  def DUPLICATE_IDS(ids: Set[Proposal.Id]): String =
    s"Duplicate proposal ids: ${ids.toList.sorted.mkString(",")}"

  /**
   * Read a persistence proposal into a phase 1 model immutable proposal.  The
   * Queue Engine works with the immutable proposal model.  Many things can go
   * wrong here since we're dealing with hibernate and the persistence model.
   * NullPointerException and LazyInitializationException, etc. are turned into
   * a single vague "database exception" message, but at least we log what
   * happened.
   *
   * @param p the persistence proposal
   *
   * @return Validation with any errors encountered while reading the proposa.
   */
  def toImmutable(p: ps.Proposal): ValidationNel[String, im.Proposal] = {
    def psErrorMessage(e: PsError): String =
      e match {
        case BadData(msg)     => msg
        case DatabaseError(t) =>
          val msg = s"Database exception while reading proposal: ${t.getMessage}"
          Log.log(Level.WARNING, msg, t)
          msg
      }

    (for {
      p1 <- nullSafePersistence("phase 1 proposal not set") { p.getPhaseIProposal }
      m  <- nullSafePersistence("could not convert to mutable proposal") { HibernateToMutableConverter.toMutable(p1) }
    } yield {
      val p = im.Proposal(m)
      p.copy(observations = p.nonEmptyObservations)
    }).leftMap(psErrorMessage(_).wrapNel).validation
  }

  // A result of loading a single persistence proposal.  It pairs the original
  // persistence proposal with a ValidationNel.  Assuming the loading was
  // successful, it might produce more than one Queue Engine proposal (if there
  // are multiple sites for example).
  type Result = (ps.Proposal, ValidationNel[String, NonEmptyList[Proposal]])

  /**
   * Performs a duplicate id check on the given proposal results, checking
   * whether the same proposal id appears in more than one proposal.
   */
  private def validate(results: List[Result]): PsError \/ List[Result] = {
    def ids(props: NonEmptyList[Proposal]): Set[Proposal.Id] =
      (Set.empty[Proposal.Id]/:props.toList) { (s,p) =>
        p match {
          case j: JointProposal => (s/:j.toParts) { (s0, jp) => s0 + jp.id }
          case _ => s + p.id
        }
      }

    val idSets = results.collect {
      case (_, Success(props)) => ids(props)
    }

    case class IdValidity(allIds: Set[Proposal.Id], duplicates: Set[Proposal.Id]) {
      def +(ids: Set[Proposal.Id]): IdValidity =
        IdValidity(allIds.union(ids), duplicates.union(allIds.intersect(ids)))
    }

    val valid = (IdValidity(Set.empty, Set.empty)/:idSets) { _ + _ }

    if (valid.duplicates.isEmpty) results.right
    else BadData(DUPLICATE_IDS(valid.duplicates)).left
  }
}

import ProposalLoader._

class ProposalLoader(ctx: Context, partners: Map[String, Partner]) {
  // "when" is necessary for estimating non-sidereal target coordinates
  private val when   = ctx.getMidpoint.getTime
  private val propIo = new ProposalIo(partners)

  /**
   * Load a list of persistence proposals.  Failures are kept up with but don't
   * stop subsequent proposals from loading.
   * @param psList persistent proposal list
   * @return list of pairs of (persistent proposal, load results)
   */
  def load(psList: List[ps.Proposal]): PsError \/ List[Result] = {
    def loadOne(p: ps.Proposal, idGen: JointIdGen): ValidationNel[String, (NonEmptyList[Proposal], JointIdGen)] =
      toImmutable(p).fold(_.failure, propIo.read(_, when, idGen))

    val zero = (List.empty[Result], JointIdGen(0))
    val (res,_) = (zero/:psList) { case ((results, gen), ps) =>
      loadOne(ps, gen) match {
        case Success((props, nextGen)) =>
          val thisResult = (ps, props.successNel[String])
          (thisResult :: results, nextGen)
        case Failure(msgNel)           =>
          val thisResult = (ps, msgNel.failure)
          (thisResult :: results, gen)
      }
    }
    validate(res)
  }
}
