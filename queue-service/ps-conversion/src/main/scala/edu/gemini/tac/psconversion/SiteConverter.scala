package edu.gemini.tac.psconversion

import edu.gemini.tac.{persistence => ps}
import edu.gemini.tac.qengine.ctx.Site

import scalaz._

/**
 * Parses a String into a SiteDesc, looking for "north", "gn", "south", or "gs"
 * and ignoring case
 */
object SiteConverter {
  def UNSPECIFIED_SITE: String       = "Unspecified site information"
  def UNRECOGNIZED_SITE(str: String) = s"Unrecognized site string '$str'"

  /**
   * Returns the matching SiteDesc based on the contents of the given String,
   * or else a ConfigError if it could not match either site.
   */
  def parse(siteStr: String): PsError \/ Site =
    nullSafe(UNRECOGNIZED_SITE(siteStr)) { Site.parse(siteStr) }

  def read(psSite: ps.Site): PsError \/ Site =
    for {
      n <- nullSafePersistence(UNSPECIFIED_SITE) { psSite.getDisplayName }
      s <- parse(n)
    } yield s

  def read(psQueue: ps.queues.Queue): PsError \/ Site =
    for {
      ps <- nullSafePersistence(UNSPECIFIED_SITE) { psQueue.getSite }
      s  <- read(ps)
    } yield s
}