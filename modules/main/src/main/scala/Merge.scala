// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import javax.xml.bind.{ JAXBContext, Marshaller, Unmarshaller }
import cats.data.NonEmptyList
import edu.gemini.model.p1.mutable._
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import scala.collection.JavaConverters._

/**
 * Merge a list of MUTABLE phase-1 proposals (typically joint parts) into a single phase-1 proposal,
 * which will always be new and will share no references with the constituent proposals. Some
 * properties that are unused at Phase 2 are omitted.
 */
object Merge {

  private lazy val context: JAXBContext =
    JAXBContext.newInstance((new ObjectFactory).createProposal().getClass())

  private lazy val marshaller: Marshaller =
    context.createMarshaller()

  private lazy val unmarshaller: Unmarshaller =
    context.createUnmarshaller()

  /** Clone a proposal by running it through the XML serializer and back. */
  private def clone(p: Proposal): Proposal = {
    val baos = new ByteArrayOutputStream
    marshaller.marshal(p, baos)
    val bais = new ByteArrayInputStream(baos.toByteArray)
    unmarshaller.unmarshal(bais).asInstanceOf[Proposal]
  }

  /** Clone a TimeAmount. */
  private def clone(t: TimeAmount): TimeAmount = {
    val ret = new TimeAmount
    ret.setUnits(t.getUnits())
    ret.setValue(t.getValue())
    ret
  }

  /**
   * Find the next id from a list of ids of the form prefix-<int>. Throws an exception if these
   * expectations aren't met.
   */
  private def nextId(prefix: String, ids: Iterable[String]): String = {
    val i: Int =
      ids.map(_.split("-")).map { case Array(_, n) => n.toInt } match {
        case Nil => 0
        case ns  => ns.max + 1
      }
      s"$prefix-$i"
    }


  /**
   * Merge a list of proposals together, returning a *new* Proposal. The passed proposals are
   * left untouched.
   */
  def merge(ps: NonEmptyList[Proposal]): Proposal = {
    val result = clone(ps.head)
    ps.tail.foreach(mergeInto(_, result))
    result
  }

  /** Find or destructively create matching `Condition` in `into` */
  def canonicalize(from: Condition, into: Proposal): Condition =
    into.getConditions().getCondition().asScala.find { c =>
      c.getCc()         == from.getCc()         &&
      c.getIq()         == from.getIq()         &&
      c.getMaxAirmass() == from.getMaxAirmass() &&
      c.getSb()         == from.getSb()         &&
      c.getWv()         == from.getWv()
    } match {
      case Some(conds) => conds
      case None        =>
        val c = new Condition
        c.setCc(from.getCc())
        c.setId(nextId("conditions", into.getConditions().getCondition().asScala.map(_.getId())))
        c.setIq(from.getIq())
        c.setMaxAirmass(from.getMaxAirmass())
        c.setName(from.getName())
        c.setSb(from.getSb())
        c.setWv(from.getWv())
        into.getConditions().getCondition().add(c) // important!
        c
    }

  /** Find or destructively create matching `Target` in `into` */
  def canonicalize(from: Target, into: Proposal): Target = {
    ???
  }

  /** Find or destructively create matching `Condition` in `into` */
  def canonicalize(from: BlueprintBase, into: Proposal): BlueprintBase = {
    ???
  }

  /** Destructively merge an observation into a proposal */
  def mergeInto(from: Observation, into: Proposal): Unit = {

    // Canonicalize the shared elements.
    val conds     = canonicalize(from.getCondition(), into)
    val target    = canonicalize(from.getTarget(), into)
    val blueprint = canonicalize(from.getBlueprint(), into)

    // See if there's an existing observation in the same band, sharing <conds, target, blueprint>.
    if (into.getObservations().getObservation().asScala.exists { o =>
      (o.getBand      == from.getBand) &&
      (o.getCondition eq conds)        &&
      (o.getTarget    eq target)       &&
      (o.getBlueprint eq blueprint)
    }) {

      // Nothing to do because `from` already exists in `into`.
      // This block intentionally left empty.

    } else {

        // `from` doesn't exist here, so make a copy of it, using the shared elements
        // created/located above.
        val o = new Observation

        // These properties are shared, so we use canonicalized references.
        o.setBlueprint(blueprint)
        o.setCondition(conds)
        o.setTarget(target)

        // These aren't shared so we don't need to canonicalize, however we do need to clone any
        // values that are mutable.
        o.setBand(from.getBand())    // immutable
        o.setGuide(null)             // unused, always null
        o.setMeta(null)             // unused at Phase 2
        o.setPartTime(clone(from.getPartTime()))
        o.setProgTime(clone(from.getProgTime()))
        o.setTime(clone(from.getTime()))

        // Done.
        into.getObservations().getObservation().add(o)
        ()

    }

  }

  /**
   * Destructively merge the contents of `from` into `into`. This is how we combine NTAC
   * informatoin from joint proposal parts.
   */
  def mergeInto(from: ProposalClassChoice, into: ProposalClassChoice): Unit = {
    ???
  }

  /** Destructively merge the contents of `from` into `into`, yielding `into` */
  def mergeInto(from: Proposal, into: Proposal): Unit = {
    // into.setAbstract()        - we assume it's the same for all
    // into.setBlueprints()      - done on the fly when merging observations
    // into.setConditions()      - done on the fly when merging observations
    // into.setInvestigators()   - we assume it's the same for all
    // into.setKeywords()        - we assume it's the same for all
    // into.setMeta()            - we assume it's the same for all

    // Merge in from's observations, one by one
    from.getObservations().getObservation().forEach(mergeInto(_, into))

    mergeInto(from.getProposalClass(), into.getProposalClass())

    // into.setScheduling()      - we assume it's the same for all
    // into.setSchemaVersion()   - we assume it's the same for all
    // into.setSemester()        - we assume it's the same for all
    // into.setTacCategory()     - we assume it's the same for all
    // into.setTargets()         - done on the fly when merging observations
    // into.setTitle()           - we assume it's the same for all
  }

}