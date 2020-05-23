// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import scala.collection.JavaConverters._
import edu.gemini.model.p1.mutable._
import gsp.math._

/**
 * Utility to create a summary of the internal structure of a mutable P1 proposal, showing sharing.
 * This is useful for showing the effect of edits.
 */
object SummaryDebug {

  def summary(c: Condition): String = {
    f"- ${c.getId()}%-12s ${c.getCc()}%-5s ${c.getIq()}%-5s ${c.getSb()}%-5s ${c.getWv()}%-5s"
  }

  def summary(d: DegDegCoordinates): String = {
    val ra  = HourAngle.HMS(Angle.hourAngle.get(Angle.fromDoubleDegrees(d.getRa().doubleValue))).format
    val dec = Declination.fromAngle.getOption(Angle.fromDoubleDegrees(d.getDec().doubleValue())).map(Declination.fromStringSignedDMS.reverseGet).getOrElse(sys.error(s"unpossible: invalid declination"))
    s"$ra $dec"
  }

  def summary(t: Target): String = {
    t match {
      case t: SiderealTarget    => f"- ${t.getId}%-12s ${summary(t.getDegDeg())} ${t.getName}"
      case t: NonSiderealTarget => f"- ${t.getId}%-12s <Non-Sidereal>                   ${t.getName}"
      case t: TooTarget         => f"- ${t.getId}%-12s <ToO>                            ${t.getName}"
    }
  }

  def summary(o: Observation): String = {
    f"* ${o.getBand}%-8s ${summary(o.getCondition)} ${summary(o.getTarget)}"
  }

  def summary(p: Proposal): String = {
    f"""|Conditions:
        |${p.getConditions().getCondition().asScala.map(summary).mkString("\n")}
        |
        |Targets:
        |${p.getTargets().getSiderealOrNonsiderealOrToo().asScala.map(summary).mkString("\n")}
        |
        |Observations:
        |${p.getObservations().getObservation().asScala.map(summary).mkString("\n")}
        |""".stripMargin
  }

}