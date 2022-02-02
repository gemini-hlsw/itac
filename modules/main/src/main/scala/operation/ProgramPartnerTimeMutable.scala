// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats._
import cats.implicits._
import edu.gemini.model.p1.mutable._
import edu.gemini.model.p1.mutable.TimeUnit._
import java.math.BigDecimal
import scala.jdk.CollectionConverters._
import java.math.MathContext

object ProgramPartnerTimeMutable {
  import EmailGen.ProposalOps

  private val HrPerNight = new BigDecimal(10)

  private implicit val MonoidTimeAmount: Monoid[TimeAmount] =
    new Monoid[TimeAmount] {
      def combine(x: TimeAmount, y: TimeAmount): TimeAmount = {
        val (t, u) = (x.getUnits, y.getUnits) match {
          case (HR, HR)       => (x.getValue.add(y.getValue), HR)
          case (NIGHT, NIGHT) => (x.getValue.add(y.getValue), NIGHT)
          case (HR, NIGHT)    => (x.getValue.add(y.getValue.multiply(HrPerNight)), HR)
          case (NIGHT, HR)    => (x.getValue.multiply(HrPerNight).add(y.getValue), HR)
        }
        val ta = new TimeAmount
        ta.setUnits(u)
        ta.setValue(t)
        ta
      }
      val empty: TimeAmount = {
        val ta = new TimeAmount
        ta.setUnits(HR)
        ta.setValue(BigDecimal.ZERO)
        ta
      }
    }

  implicit class TimeAmountOps(ta: TimeAmount) {

    def toHours: TimeAmount =
      ta.getUnits match {
        case HR    => ta
        case NIGHT =>
          val taʹ = new TimeAmount
          taʹ.setUnits(HR)
          taʹ.setValue(ta.getValue.multiply(HrPerNight))
          taʹ
      }

    def scale(factor: BigDecimal): TimeAmount = {
      val taʹ = new TimeAmount
      taʹ.setUnits(ta.getUnits)
      taʹ.setValue(ta.getValue.multiply(factor))
      taʹ
    }

  }

  def programAndPartnerTime(p: Proposal): (TimeAmount, TimeAmount) = {

    // Need our band
    val band: Band =
      p.getItacAccept.getBand match {
        case 3 => Band.BAND_3
        case _ => Band.BAND_1_2
      }

    // Original estimated times from the proposal
    val (progTime, partTime) =
      p.getObservations.getObservation.asScala.toList
        .filter(_.getBand == band)
        .foldMap(o => (o.getProgTime, o.getPartTime))

    // Factor with respect to the awarded time
    val ratio: BigDecimal = progTime.toHours.getValue.divide((progTime |+| partTime).toHours.getValue, MathContext.DECIMAL32)

    // Scale the prog and program time
    val progTimeʹ = p.getItacAccept.getAward.scale(ratio)
    val partTimeʹ = p.getItacAccept.getAward.scale(BigDecimal.ONE.subtract(ratio))

    // Done
    (progTimeʹ, partTimeʹ)

  }

}