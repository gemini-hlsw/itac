package edu.gemini.tac.qservice.impl.queue

import edu.gemini.tac.persistence.daterange.{Shutdown => PsShutdown}
import edu.gemini.tac.psconversion._
import edu.gemini.tac.qengine.api.config.Shutdown

import scalaz._

object ShutdownExtractor {
  def extract(psShutdown: PsShutdown): PsError \/ Shutdown =
    for {
      psSite <- nullSafePersistence("Shutdown site not specified") { psShutdown.getSite }
      site   <- SiteConverter.read(psSite)
      start  <- nullSafePersistence("Shutdown start date missing") { psShutdown.getStart }
      end    <- nullSafePersistence("Shutdown end date missing")   { psShutdown.getEnd }
    } yield Shutdown(site, start, end)
}
