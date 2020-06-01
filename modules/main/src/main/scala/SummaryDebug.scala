// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats.implicits._
import scala.collection.JavaConverters._
import edu.gemini.model.p1.mutable._
import gsp.math._

/**
 * Utility to create a summary of the internal structure of a mutable P1 proposal, showing sharing.
 * This is useful for showing the effect of edits.
 */
object SummaryDebug {

  def summary(pc: ProposalClass): String =
    pc.getClass.getName

  def summary(acc: SubmissionAccept): String =
    if (acc == null) ""
    else f"${acc.getRecommend.getValue().doubleValue()}%5.1f ${acc.getRecommend().getUnits()}   Rank ${acc.getRanking()}"

  def summary(rej: SubmissionReject): String =
    if (rej == null) "" else "Reject"

  def summary(res: SubmissionResponse): String =
    if (res == null) "--"
    else f"${res.getReceipt.getId}%-15s ${summary(res.getAccept())}${summary(res.getReject())} "

  def summary(sub: NgoSubmission): String =
    s"- ${sub.getPartner()} ${summary(sub.getResponse)}"

  def summary(itac: Itac): String =
    if (itac == null) "--"
    else s"""|- Accept:  ${itac.getAccept()} / ${itac.getReject()}
             |        - Comment: ${itac.getComment()}
             |        - NGO:     ${itac.getNgoauthority()}
             |""".stripMargin.trim

  def summary(pc: QueueProposalClass): String =
    s"""|- TooOption: ${pc.getTooOption()}
        |- Exchange: ${pc.getExchange()}
        |- NGO:  ${pc.getNgo().asScala.toList.map(summary).mkString("\n        ")}
        |- ITAC: ${summary(pc.getItac())}
        |""".stripMargin.trim

  def summary(pc: ProposalClassChoice): String = {
    Option(pc.getClassical)     .map(summary) <+>
    Option(pc.getExchange)      .map(summary) <+>
    Option(pc.getFastTurnaround).map(summary) <+>
    Option(pc.getLarge)         .map(summary) <+>
    Option(pc.getQueue)         .map(summary) <+>
    Option(pc.getSip)           .map(summary) <+>
    Option(pc.getSpecial)       .map(summary)
  } .get

  def summary(c: Condition): String = {
    f"- ${c.getId}%-12s ${c.getCc}%-5s ${c.getIq}%-5s ${c.getSb}%-5s ${c.getWv}%-5s"
  }

  def summary(d: DegDegCoordinates): String = {
    val ra  = HourAngle.HMS(Angle.hourAngle.get(Angle.fromDoubleDegrees(d.getRa.doubleValue))).format
    val dec = Declination.fromAngle.getOption(Angle.fromDoubleDegrees(d.getDec.doubleValue)).map(Declination.fromStringSignedDMS.reverseGet).getOrElse(sys.error(s"unpossible: invalid declination"))
    s"$ra $dec"
  }

  def summary(t: Target): String = {
    t match {
      case t: SiderealTarget    => f"- ${t.getId}%-12s ${summary(t.getDegDeg)} ${t.getName}"
      case t: NonSiderealTarget => f"- ${t.getId}%-12s <Non-Sidereal>                   ${t.getName}"
      case t: TooTarget         => f"- ${t.getId}%-12s <ToO>                            ${t.getName}"
    }
  }

  def summary(o: Observation): String = {
    f"* ${o.getBand}%-8s ${summary(o.getCondition)} ${summary(o.getTarget)}"
  }

  def summary(b: BlueprintBase): String =
    f"* ${b.getId}%-15s ${b.getName}"

  def summary(p: Proposal): String = {
    f"""|ProposalClass:
        |${summary(p.getProposalClass)}
        |
        |Conditions:
        |${p.getConditions.getCondition.asScala.map(summary).mkString("\n")}
        |
        |Targets:
        |${p.getTargets.getSiderealOrNonsiderealOrToo.asScala.map(summary).mkString("\n")}
        |
        |Blueprints:
        |${Merge.allBlueprints(p).map(summary).mkString("\n")}
        |
        |Observations:
        |${p.getObservations.getObservation.asScala.map(summary).mkString("\n")}
        |""".stripMargin
  }

}