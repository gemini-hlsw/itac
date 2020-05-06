// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac
package operation

import cats._
import cats.implicits._
import edu.gemini.tac.qengine.api.config._
import edu.gemini.tac.qengine.api.QueueEngine
import java.nio.file.Path
import edu.gemini.tac.qengine.ctx.Context
import edu.gemini.qengine.skycalc.RaBinSize
import edu.gemini.qengine.skycalc.DecBinSize
import edu.gemini.qengine.skycalc.RaDecBinCalc
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.util.Percent
import edu.gemini.tac.qservice.impl.shutdown.ShutdownCalc
import edu.gemini.tac.qengine.api.QueueCalc
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.api.queue.ProposalQueue
// import edu.gemini.tac.qengine.p1.CoreProposal
// import edu.gemini.tac.qengine.p1.JointProposal
// import edu.gemini.tac.qengine.p1.JointProposalPart
import edu.gemini.model.p1.immutable.{ Proposal => P1Proposal }
import edu.gemini.model.p1.immutable.ItacAccept
import edu.gemini.model.p1.immutable.Itac
import edu.gemini.model.p1.immutable.ItacReject
import edu.gemini.model.p1.immutable.TimeAmount
import edu.gemini.model.p1.immutable.`package`.TimeUnit
import itac.config.Edit.lenses._
import edu.gemini.tac.qengine.p1.CoreProposal
import edu.gemini.tac.qengine.p1.JointProposal
import edu.gemini.tac.qengine.p1.JointProposalPart

abstract class AbstractQueueOperation[F[_]](
  qe:             QueueEngine,
  siteConfig:     Path,
  rolloverReport: Option[Path]
) extends Operation[F] {

  def computeQueue(ws: Workspace[F])(
    implicit ev: FlatMap[F]
  ): F[(List[Proposal], QueueCalc)] =
    for {

      // Load the things we need
      cc <- ws.commonConfig
      qc <- ws.queueConfig(siteConfig)
      rr <- ws.readRolloverReport(rolloverReport.getOrElse(s"${qc.site.abbreviation.toLowerCase}-rollovers.yaml"))
      ps <- ws.proposals

      // Compute the queue
      partners  = cc.engine.partners
      queueCalc = qe.calc(
        proposals = ps,
        // queueTime = qc.engine.queueTime(partners),
        queueTime = qc.engine.explicitQueueTime(partners),
        partners  = partners,
        config    = QueueEngineConfig(
          partners   = partners,
          partnerSeq = cc.engine.partnerSequence(qc.site),
          rollover   = rr,
          binConfig  = createConfig(
            ctx        = Context(qc.site, cc.semester),
            ra         = qc.raBinSize,
            dec        = qc.decBinSize,
            shutdowns  = cc.engine.shutdowns(qc.site),
            conditions = cc.engine.conditionsBins
          ),
          restrictedBinConfig = RestrictionConfig(
            relativeTimeRestrictions = Nil, // TODO
            absoluteTimeRestrictions = Nil, // TODO
            bandRestrictions         = Nil, // TODO
          )
        ),
      )

      // Now we need to set the Itac information for each proposal's associated p1Proposal.
      psʹ = ps.map(addOrUpdateItacNode(_, queueCalc.queue))

    } yield (psʹ, queueCalc)

  // Given a Proposal and a ProposalQueue, update the Proposal's p1proposal's Itac element, or add
  // one if not present.
  def addOrUpdateItacNode(p: Proposal, q: ProposalQueue): Proposal = {

    // P1Proposal, which should always be present
    val p1 = p.p1proposal

    // Our decision, based on presence of a queue position.
    val decision: Either[ItacReject, ItacAccept] =
      (q.programId(p), q.positionOf(p)).tupled match {
        case Some((pid, pos)) => Right(ItacAccept(
          programId = pid.toString,
          contact   = Some("Bob Dobbs"),                                              // TODO
          email     = Some("bob@dobbs.com"),                                              // TODO
          band      = pos.band.number,
          award     = new TimeAmount(p.time.toHours.value, TimeUnit.HR),
          rollover  = false                                              // TODO
        ))
        case None             => Left(ItacReject)
        case _                => sys.error("unpossible")
      }

    // New ITAC node
    val itac  = p1.proposalClass.itac.getOrElse(Itac(None, None, None))
    val itacʹ = itac.copy(
      decision     = Some(decision),
      ngoAuthority = None // TODO
    )

    // New P1Proposal
    val p1ʹ= P1Proposal.itac.set(p1, Some(itacʹ))

    // New Proposal
    val pʹ = p match {
      case cp: CoreProposal      => cp.copy(p1proposal = p1ʹ)
      case jp: JointProposal     => jp.copy(core = jp.core.copy(p1proposal = p1ʹ))
      case pp: JointProposalPart => pp.copy(core = pp.core.copy(p1proposal = p1ʹ))
    }

    // Done
    pʹ

  }


  // These methods were lifted from the ITAC web application.

  private def shutdownHours(shutdowns : List[Shutdown], ctx: Context, size: RaBinSize): List[Time] =
    ShutdownCalc.sumHoursPerRa(ShutdownCalc.trim(shutdowns, ctx), size)

  private def createConfig(ctx: Context, ra: RaBinSize, dec: DecBinSize, shutdowns: List[Shutdown], conditions: ConditionsBinGroup[Percent]): SiteSemesterConfig = {
    import scala.collection.JavaConverters._
    val calc = RaDecBinCalc.get(ctx.site, ctx.semester, ra, dec)
    val hrs  = calc.getRaHours.asScala.map(h => Time.hours(h.getHours))
    val perc = calc.getDecPercentages.asScala.map(p => Percent(p.getAmount.round.toInt.toDouble))
    val hrsʹ = hrs.zip(shutdownHours(shutdowns, ctx, ra)).map { case (t1, t2) => (t1 - t2).max(Time.ZeroHours) }
    new SiteSemesterConfig(ctx.site, ctx.semester, RaBinGroup(hrsʹ), DecBinGroup(perc), shutdowns, conditions)
  }

}
