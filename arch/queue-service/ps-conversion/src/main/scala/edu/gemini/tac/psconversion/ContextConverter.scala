package edu.gemini.tac.psconversion

import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.qengine.ctx.Context
import scalaz._

/**
 * Extracts SiteSemesterConfig from the persistence Queue object.
 */
object ContextConverter {
  def read(queue: PsQueue): PsError \/ Context =
    for {
      site     <- SiteConverter.read(queue)
      semester <- SemesterConverter.read(queue)
    } yield new Context(site, semester)
}