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
      es <- ws.extras
      xs <- ws.extrasNotSubmitted
      rs <- ws.removed

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
          ),
          explicitQueueAssignments = qc.explicitAssignments.getOrElse(Map.empty)
        ),
        extras  = es ++ xs,
        removed = rs,
      )

    } yield (ps ++ es ++ xs ++ rs, queueCalc)

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
