package edu.gemini.tac.qservice.impl.queue

import edu.gemini.tac.persistence.bin.{BinConfiguration => PsBinConfiguration}
import edu.gemini.tac.persistence.daterange.{Shutdown => PsShutdown}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.psconversion._

import edu.gemini.qengine.skycalc._

import edu.gemini.tac.qengine.api.config.{Shutdown, DecBinGroup, RaBinGroup, SiteSemesterConfig}
import edu.gemini.tac.qengine.ctx.Context
import edu.gemini.tac.qengine.util.{Percent, Time}
import edu.gemini.tac.qservice.impl.shutdown.ShutdownCalc

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/**
 * Extracts SiteSemesterConfig from the persistence Queue object.
 */
object SiteSemesterExtractor {

  def UNSPECIFIED_BIN_CONFIG: String = "Unspecified bin configuration"
  def UNSPECIFIED_RA_SIZE: String    = "Unspecified RA bin size"
  def UNSPECIFIED_DEC_SIZE: String   = "Unspecified dec bin size"

  def extractRaBinSize(binConf: PsBinConfiguration): PsError \/ RaBinSize =
    for {
      psSize <- nullSafePersistence(UNSPECIFIED_RA_SIZE) { binConf.getRaBinSize }
      raSize <- RaBinSizeExtractor.extract(psSize)
    } yield raSize

  def extractDecBinSize(binConf: PsBinConfiguration): PsError \/ DecBinSize =
    for {
      psSize  <- nullSafePersistence(UNSPECIFIED_DEC_SIZE) { binConf.getDecBinSize }
      decSize <- DecBinSizeExtractor.extract(psSize)
    } yield decSize

  private def convertPersistenceShutdowns(shutdowns : List[PsShutdown]): PsError \/ List[Shutdown] =
    shutdowns.map { ShutdownExtractor.extract }.sequenceU

  private def shutdownHours(shutdowns : List[Shutdown], ctx: Context, size: RaBinSize): List[Time] =
    ShutdownCalc.sumHoursPerRa(ShutdownCalc.trim(shutdowns, ctx), size)

  private def createConfig(ctx: Context, ra: RaBinSize, dec: DecBinSize, shutdowns: List[Shutdown]): SiteSemesterConfig = {
    val calc = RaDecBinCalc.get(ctx.getSite, ctx.getSemester, ra, dec)
    val hrs  = calc.getRaHours.asScala.map((h: Hours) => Time.hours(h.getHours))
    val perc = calc.getDecPercentages.asScala.map(p => Percent(p.getAmount.round.toInt))
    val adjHrs = hrs.zip(shutdownHours(shutdowns, ctx, ra)).map { case (t1, t2) => (t1 - t2).max(Time.ZeroHours) }
    new SiteSemesterConfig(ctx.getSite, ctx.getSemester, RaBinGroup(adjHrs), DecBinGroup(perc), shutdowns)
  }

  def extract(ctx: Context, binConf: PsBinConfiguration, psShutdowns: List[PsShutdown]): PsError \/ SiteSemesterConfig =
    for {
      raSize   <- extractRaBinSize(binConf)
      decSize  <- extractDecBinSize(binConf)
      sds      <- convertPersistenceShutdowns(psShutdowns)
    } yield createConfig(ctx, raSize, decSize, sds)

  def extract(queue: PsQueue): PsError \/ SiteSemesterConfig =
    for {
      ctx <- ContextConverter.read(queue)
      bc  <- nullSafePersistence(UNSPECIFIED_BIN_CONFIG) { queue.getBinConfiguration }
      c   <- nullSafePersistence("Queue has no committee") { queue.getCommittee }
      sds <- safePersistence(Option(c.getShutdowns).fold(List.empty[PsShutdown])(_.asScala.toList))
      ss  <- extract(ctx, bc, sds)
    } yield ss
}
