// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats.implicits._
import scala.collection.JavaConverters._
import edu.gemini.model.p1.mutable._
import gsp.math._
import io.circe._
import io.circe.syntax._
import scala.math.BigDecimal.RoundingMode

/**
 * Utility to create a summary of the internal structure of a mutable P1 proposal, showing sharing.
 * This is useful for showing the effect of edits.
 */
object SummaryDebugJson {

  implicit def encoderEnum[A <: Enum[A]]: Encoder[A] = a =>
    Json.fromString(a.name)

  implicit val EncoderProposalClass: Encoder[ProposalClass] =
    Encoder[String].contramap(_.getClass.getName)

  def summary(ta: TimeAmount): String =
    s"${ta.getValue.setScale(1, RoundingMode.HALF_UP)} ${ta.getUnits}"

  implicit val EncoderTimeAmount: Encoder[TimeAmount] = ta =>
    summary(ta).asJson
    // Json.obj(
    //   "Value" -> ta.getValue.setScale(1, RoundingMode.HALF_UP).asJson,
    //   "Units" -> ta.getUnits.asJson,
    // )

  implicit val EncoderSubmissionAccept: Encoder[SubmissionAccept] = acc =>
    Json.obj(
      "Recommend" -> acc.getRecommend.asJson,
      "Email"     -> acc.getEmail.asJson,
      "Ranking"   -> acc.getRanking.asJson,
    )

  implicit val EncoderSubmissionReject: Encoder[SubmissionReject] = _ =>
    "Reject".asJson

  implicit val EncoderSubmissionReceipt: Encoder[SubmissionReceipt] = rec =>
    Json.obj(
      "Contact" -> Option(rec.getContact).asJson,
      "Id"      -> Option(rec.getId).asJson,
    )

  implicit val EncoderSubmissionResponse: Encoder[SubmissionResponse] = res =>
    Json.obj(
      "Receipt" -> Option(res.getReceipt).asJson,
      "Accept"  -> Option(res.getAccept).asJson,
      "Reject"  -> Option(res.getReject).asJson,
      "Comment" -> Option(res.getComment).asJson,
    )

  implicit val EncoderInvestigator: Encoder[Investigator] = inv =>
    Json.fromString(s"${inv.getLastName} #<${System.identityHashCode(inv).toHexString}>")
    // Json.obj(
    //   "Email"     -> inv.getEmail.asJson,
    //   "FirstName" -> inv.getFirstName.asJson,
    //   "LastName"  -> inv.getLastName.asJson,
    //   "Status"    -> inv.getStatus.asJson,
    //   "HashCode"  -> System.identityHashCode(inv).asJson,
    // )

  implicit val EncoderPrincipalInvestigator: Encoder[PrincipalInvestigator] = inv =>
    Json.obj(
      "LastName" -> Json.fromString(s"${inv.getLastName} #<${System.identityHashCode(inv).toHexString}>"),
      "InstitutionAddress" -> Json.obj(
        "Institution" -> inv.getAddress().getInstitution.asJson,
        "Country"     -> inv.getAddress().getCountry.asJson,
      ),
    )

  implicit val EncoderSubmissionRequest: Encoder[SubmissionRequest] = _ =>
    "<not implemented>".asJson

  implicit val EncoderNgoSubmission: Encoder[NgoSubmission] = sub =>
    Json.obj(
      "Partner"     -> Option(sub.getPartner).asJson,
      "PartnerLead" -> Option(sub.getPartnerLead).asJson,
      // "Request"     -> Option(sub.getRequest).asJson,
      "Response"    -> Option(sub.getResponse).asJson,
    )

  implicit val EncoderItacReject: Encoder[ItacReject] = _ =>
    "Reject".asJson

  implicit val EncoderItacAccept: Encoder[ItacAccept] = acc =>
    Json.obj(
      "Award"            -> Option(acc.getAward).asJson,
      "Band"             -> Option(acc.getBand).asJson,
      "Contact (Gemini)" -> Option(acc.getContact).asJson,
      "Email (NGO)"      -> Option(acc.getEmail).asJson,
      "ProgramId"        -> Option(acc.getProgramId).asJson,
    )

  implicit val EncoderItac: Encoder[Itac] = itac =>
    Json.obj(
      "Accept"       -> Option(itac.getAccept).asJson,
      "Reject"       -> Option(itac.getReject).asJson,
      "Comment"      -> Option(itac.getComment).asJson,
      "NgoAuthority" -> Option(itac.getNgoauthority).asJson,
    )

  implicit val EncoderQueueProposalClass: Encoder[QueueProposalClass] = pc =>
    Json.obj(
      // "Band3Request" -> Option(pc.getBand3Request).asJson,
      "TooOption"    -> Option(pc.getTooOption).asJson,
      // "Comment"      -> Option(pc.getComment).asJson,
      // "Exchange"     -> "<not implemented>".asJson,
      "Ngo"          -> Option(pc.getNgo).map(_.asScala).asJson,
      "Itac"         -> Option(pc.getItac).asJson,
    )

  implicit val EncoderProposalClassChoice: Encoder[ProposalClassChoice] = pc =>
    Json.obj(
      "Classical"      -> Option(pc.getClassical).widen[ProposalClass].asJson,
      "Exchange"       -> Option(pc.getExchange).widen[ProposalClass].asJson,
      "FastTurnaround" -> Option(pc.getFastTurnaround).widen[ProposalClass].asJson,
      "Large"          -> Option(pc.getLarge).widen[ProposalClass].asJson,
      "Queue"          -> Option(pc.getQueue).asJson, // only specialized one right now
      "Sip"            -> Option(pc.getSip).widen[ProposalClass].asJson,
      "Special"        -> Option(pc.getSpecial).widen[ProposalClass].asJson,
    )

  implicit val EncoderCondition: Encoder[Condition] = c =>
    f"${c.getId}%-15s ${c.getCc}%-5s ${c.getIq}%-5s ${c.getSb}%-5s ${c.getWv}%-5s".asJson

  def summary(d: DegDegCoordinates): String = {
    val ra  = HourAngle.HMS(Angle.hourAngle.get(Angle.fromDoubleDegrees(d.getRa.doubleValue))).format
    val dec = Declination.fromAngle.getOption(Angle.fromDoubleDegrees(d.getDec.doubleValue)).map(Declination.fromStringSignedDMS.reverseGet).getOrElse(sys.error(s"unpossible: invalid declination"))
    s"$ra $dec"
  }

  implicit val EncodeTarget: Encoder[Target] = t => {
    t match {
      case t: SiderealTarget    => f"${t.getId}%-15s ${summary(t.getDegDeg)} ${t.getName}"
      case t: NonSiderealTarget => f"${t.getId}%-15s <Non-Sidereal>                   ${t.getName}"
      case t: TooTarget         => f"${t.getId}%-15s <ToO>                            ${t.getName}"
    }
  } .asJson

  implicit val EncoderObservation: Encoder[Observation] = o =>
    f"${o.getBand}%-9s ${o.getBlueprint().getId()}%-15s ${o.getCondition().getId()}%-15s ${o.getTarget().getId()}%-15s ${summary(o.getTime)}".asJson

  implicit val EncoderBlueprintBase: Encoder[BlueprintBase] = b =>
    f"${b.getId}%-15s ${b.getName}".asJson

  implicit val EncoderInvestigators: Encoder[Investigators] = is =>
    Json.obj(
      "Pi"  -> is.getPi.asJson,
      "Coi" -> Option(is.getCoi).map(_.asScala.toList.widen[Investigator]).asJson,
    )

  implicit val EncoderProposal: Encoder[Proposal] = p =>
    Json.obj(
      "Investigators" -> p.getInvestigators.asJson,
      "Targets"       -> p.getTargets().getSiderealOrNonsiderealOrToo().asScala.asJson,
      "Conditions"    -> p.getConditions().getCondition().asScala.asJson,
      "Blueprints"    -> Merge.allBlueprints(p).asJson,
      "Observations"  -> p.getObservations().getObservation().asScala.asJson,
      "Scheduling"    -> Option(p.getScheduling().trim).filterNot(_.isEmpty).asJson,
      "ProposalClass" -> p.getProposalClass.asJson,
    )

  // def summary(p: Proposal): Json = {
  //   f"""|PI: ${p.getInvestigators.getPi.getLastName} ... ${System.identityHashCode(p.getInvestigators.getPi)}
  //       |
  //       |ProposalClass:
  //       |  ${summary(p.getProposalClass)}
  //       |
  //       |Conditions:
  //       |${p.getConditions.getCondition.asScala.map(summary).mkString("\n")}
  //       |
  //       |Targets:
  //       |${p.getTargets.getSiderealOrNonsiderealOrToo.asScala.map(summary).mkString("\n")}
  //       |
  //       |Blueprints:
  //       |${Merge.allBlueprints(p).map(summary).mkString("\n")}
  //       |
  //       |Observations:
  //       |${p.getObservations.getObservation.asScala.map(summary).mkString("\n")}
  //       |""".stripMargin
  // }

}