// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.api.config

import edu.gemini.spModel.core.Site

import java.text.SimpleDateFormat
import java.util.Date

/**
 * Defines a shutdown period.
 */
case class Shutdown(site: Site, start: Date, end: Date) extends Ordered[Shutdown] {
  require(start.getTime <= end.getTime)

  def compare(that: Shutdown): Int = {
    def compareStartAndEndDates = {
      def compareDates(f: Shutdown => Date) = f(Shutdown.this).compareTo(f(that))
      val res = compareDates(_.start)
      if (res == 0) compareDates(_.end) else res
    }

    val res = site.compareTo(that.site)
    if (res == 0) compareStartAndEndDates else res
  }

  def toDateString: String = {
    val df = new SimpleDateFormat("MMM d")
    df.setTimeZone(site.timezone)

    "%s - %s".format(df.format(start), df.format(end))
  }

  override def toString: String =
    "%s %s".format(site.abbreviation, toDateString)
}