// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package operation

import cats._
import cats.implicits._
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.model.p1.immutable.TimeAmount
import edu.gemini.tac.qengine.util.Time

object ProgramPartnerTime {

  private implicit val m: Monoid[TimeAmount] =
    new Monoid[TimeAmount] {
      def combine(x: TimeAmount, y: TimeAmount): TimeAmount = x |+| y
      def empty: TimeAmount = TimeAmount.empty
    }

  def programAndPartnerTime(p: Proposal, band: QueueBand): (Time, Time) = {

    // Original estimated times from the proposal
    val (progTime, partTime): (TimeAmount, TimeAmount) =
      p.obsListFor(band)
       .map(_.p1Observation)
       .foldMap(o => (o.progTime.orEmpty, o.partTime.orEmpty))

    // Factor with respect to the awarded time
    val ratio: Double = progTime.toHours.value / (progTime |+| partTime).toHours.value

    // Scale the prog and program time
    val progTimeʹ = Time.millisecs((p.time.ms * ratio).toLong).toHours
    val partTimeʹ = Time.millisecs((p.time.ms * (1.0 - ratio)).toLong).toHours

    // Done
    (progTimeʹ, partTimeʹ)

  }

}