// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import javax.xml.bind.{ JAXBContext, Marshaller, Unmarshaller }
import cats.data.NonEmptyList
import cats.implicits._
import edu.gemini.model.p1.mutable._
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import scala.jdk.CollectionConverters._
import java.{util => ju}
// import itac.util.Colors

/**
 * Merge a list of MUTABLE phase-1 proposals (typically joint parts) into a single phase-1 proposal,
 * which will always be new and will share no references with the constituent proposals. Some
 * properties that are unused at Phase 2 are omitted.
 */
object Merge extends MergeBlueprint {

  private lazy val context: JAXBContext =
    JAXBContext.newInstance((new ObjectFactory).createProposal.getClass)

  private lazy val marshaller: Marshaller =
    context.createMarshaller

  private lazy val unmarshaller: Unmarshaller =
    context.createUnmarshaller

  /** Clone a proposal by running it through the XML serializer and back. */
  private def clone(p: Proposal): Proposal = {
    val baos = new ByteArrayOutputStream
    marshaller.marshal(p, baos)
    val bais = new ByteArrayInputStream(baos.toByteArray)
    unmarshaller.unmarshal(bais).asInstanceOf[Proposal]
  }

  /**
   * Merge a list of proposals together, returning a *new* Proposal. The passed proposals are
   * left untouched.
   */
  def merge(ps: NonEmptyList[Proposal]): Proposal = {
    val ret = ps.map(clone).reduceLeft((a, b) => mergeInto(b, a)) // clone everything so we don't need to copy subtrees explicitly
    clone(ret) // as a sanity chech, run through the serializer again to ensure that all the ids resolve ok
  }

  /** Find or destructively create matching `Condition` in `into` */
  def canonicalize(from: Condition, into: Proposal): Condition = {
    val col = into.getConditions.getCondition
    val all = col.asScala
    all.find { c =>
      c.getCc         == from.getCc         &&
      c.getIq         == from.getIq         &&
      c.getMaxAirmass == from.getMaxAirmass &&
      c.getSb         == from.getSb         &&
      c.getWv         == from.getWv
    } match {
      case Some(conds) => conds
      case None        =>
        from.setId(nextId("condition", all.map(_.getId)))
        col.add(from)
        from
    }
  }

  /**
   * Find or destructively create matching `Target` in `into`. We do this by looking for targets
   * with the same name. This is probably not sufficient in general but let's start with it and see.
   */
  def canonicalize(from: Target, into: Proposal): Target = {
    val col = into.getTargets.getSiderealOrNonsiderealOrToo
    val all = col.asScala
    all.find { t => t.getName == from.getName
    } match {
      case Some(t) => t
      case None    =>
        from.setId(nextId("target", all.map(_.getId)))
        col.add(from) // important!
        from
    }
  }

  /** Destructively merge an observation into a proposal, yielding `into`. */
  def mergeInto(from: Observation, into: Proposal): Unit = {
    val col = into.getObservations.getObservation

    // Canonicalize the shared elements.
    val conds     = canonicalize(from.getCondition, into)
    val target    = canonicalize(from.getTarget,    into)
    val blueprint = canonicalize(from.getBlueprint, into)

    // See if there's an existing observation in the same band, sharing <conds, target, blueprint>.
    col.asScala.find { o =>
      (o.getBand      == from.getBand) &&
      (o.getCondition eq conds)        &&
      (o.getTarget    eq target)       &&
      (o.getBlueprint eq blueprint)
    } match {
      case Some(_) =>
        // Nothing to do because `from` already exists in `into`.
        // This block intentionally left empty.
      case None =>
        // Retarget shared structure
        from.setBlueprint(blueprint)
        from.setCondition(conds)
        from.setTarget(target)
        // And add.
        col.add(from)
        ()
    }

  }

  /**
   * Merge NGO submissions, taking those with a response in `from` and replacing corresponding ones
   * without a response in `into`.
   */
  def mergeInto(from: ju.List[NgoSubmission], into: ju.List[NgoSubmission]): Unit = {
    // TODO: assert that the structures are the same (and not null!)
    into.forEach { intoSub =>
      from
        .asScala
        .find(fromSub => fromSub.getPartner == intoSub.getPartner && fromSub.getResponse != null)
        .foreach(fromSub => intoSub.setResponse(fromSub.getResponse))
    }
  }

  def mergeInto(from: ItacAccept, into: ItacAccept): Unit =
    if (from != null && into != null) {
      into.setAward {
        val ta = new TimeAmount
        ta.setUnits(into.getAward.getUnits)
        // assume same units in both
        ta.setValue(into.getAward.getValue add from.getAward.getValue)
        ta
      }
    }

  def mergeInto(from: Itac, into: Itac): Unit =
    if (from != null && into != null)
      mergeInto(from.getAccept, into.getAccept) // we assume this is here for now

  def mergeInto(from: ClassicalProposalClass, into: ClassicalProposalClass): Unit = {
    mergeInto(from.getItac, into.getItac)
    mergeInto(from.getNgo, into.getNgo)
  }

  def mergeInto(from: ExchangeProposalClass, into: ExchangeProposalClass): Unit = {
    mergeInto(from.getItac, into.getItac)
    mergeInto(from.getNgo, into.getNgo)
  }

  def mergeInto(from: QueueProposalClass, into: QueueProposalClass): Unit = {
    mergeInto(from.getItac, into.getItac)
    mergeInto(from.getNgo, into.getNgo)
  }

  /**
   * Destructively merge the contents of `from` into `into`. This is how we combine NTAC
   * informatoin from joint proposal parts.
   */
  def mergeInto(from: ProposalClassChoice, into: ProposalClassChoice): Unit = {

    def tryMerge[A](f: ProposalClassChoice => A)(g: (A, A) => Unit): Boolean =
      (Option(f(from)), Option(f(into))).tupled.map { case (a, b) => g(a, b) } .isDefined

    def nop[A](a: A, b: A): Unit = (a, b, ())._3 // defeat unused warning

    // You are in a little maze of twisty cases, all annoying.
    val merged =
      tryMerge(_.getClassical)(mergeInto) ||
      tryMerge(_.getExchange)(mergeInto)  ||
      tryMerge(_.getFastTurnaround)(nop)  ||
      tryMerge(_.getLarge)(nop)           ||
      tryMerge(_.getQueue)(mergeInto)     ||
      tryMerge(_.getSip)(nop)             ||
      tryMerge(_.getSpecial)(nop)

    // Punt. Shouldn't happen.
    if (!merged)
      sys.error("Proposal classes don't match.")

  }

  /** Destructively merge the contents of `from` into `into`, yielding `into` */
  def mergeInto(from: Proposal, into: Proposal): Proposal = {

    // import itac.util.Colors
    // println(s"${Colors.GREEN}FROM\n${SummaryDebug.summary(from)}${Colors.RESET}")
    // println(s"${Colors.YELLOW}INTO\n${SummaryDebug.summary(into)}${Colors.RESET}")

    // into.setAbstract        - we assume it's the same for all
    // into.setBlueprints      - done on the fly when merging observations
    // into.setConditions      - done on the fly when merging observations
    // into.setInvestigators   - we assume it's the same for all
    // into.setKeywords        - we assume it's the same for all
    // into.setMeta            - we assume it's the same for all

    // Merge in from's observations, one by one
    from.getObservations.getObservation.forEach(mergeInto(_, into))

    mergeInto(from.getProposalClass, into.getProposalClass)

    // into.setScheduling      - we assume it's the same for all
    // into.setSchemaVersion   - we assume it's the same for all
    // into.setSemester        - we assume it's the same for all
    // into.setTacCategory     - we assume it's the same for all
    // into.setTargets         - done on the fly when merging observations
    // into.setTitle           - we assume it's the same for all

    // println(s"${Colors.RED}RESULT\n${SummaryDebug.summary(into)}${Colors.RESET}")
    // println()
    // println()

    into
  }

}

