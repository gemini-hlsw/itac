package edu.gemini.tac.qengine.p1.io

import edu.gemini.model.p1.{immutable => im}
import edu.gemini.model.p1.{mutable => m}
import java.util.UUID

import edu.gemini.spModel.core.{Angle, Coordinates, Declination, RightAscension}

import scalaz._
import Scalaz._

class ProposalFixture {
  def oneHour       = im.TimeAmount(1.0, im.TimeUnit.HR)

  def meta          = im.Meta(None, None, band3OptionChosen = true, overrideAffiliate = false)
  def semester      = im.Semester(2014, im.SemesterOption.A)
  def title         = "Test Proposal"
  def abs           = "My test proposal abstract"
  def tacCategory   = Some(im.TacCategory.ACTIVE_GALAXIES_QUASARS_SMBH)

  def piStatus      = im.InvestigatorStatus.PH_D
  def piAddress     = im.InstitutionAddress("Bovine University")
  val piUuid        = UUID.randomUUID()
  def pi            = im.PrincipalInvestigator(piUuid, "Biff", "Henderson", Nil, "bhenderson@bovine.edu", piStatus, im.InvestigatorGender.NONE_SELECTED, piAddress)
  def investigators = im.Investigators(pi, Nil)

  def siderealCoords = Coordinates(RightAscension.fromAngle(Angle.parseHMS("10:11:12").getOrElse(Angle.zero)), Declination.fromAngle(Angle.parseDMS("-20:30:40").getOrElse(Angle.zero)).getOrElse(Declination.zero))
  val targetUuid     = UUID.randomUUID()
  def siderealTarget = im.SiderealTarget(targetUuid, "BiffStar", siderealCoords, im.CoordinatesEpoch.J_2000, None, Nil)
  def targets: List[im.Target] = List(siderealTarget)

  def blueprint: im.BlueprintBase = im.GmosSBlueprintImaging(List(m.GmosSFilter.CaT_G0333))
  def conditions     = im.Condition.empty
  def observation    = im.Observation(
    Some(blueprint),
    Some(conditions),
    Some(siderealTarget),
    im.Band.BAND_1_2,
    Some(oneHour)
  )
  def observations   = List(observation)

  def ngoPartner         = im.NgoPartner.CL
  val proposalKey        = some(UUID.randomUUID())
  def submissionId       = "abc-123"
  def submissionRequest  = im.SubmissionRequest(oneHour, oneHour, None, None)
  def partnerRanking     = 6.66
  def submissionAccept   = im.SubmissionAccept(
    "abc@123.edu",
    partnerRanking,
    oneHour,
    oneHour,
    poorWeather = false
  )
  def submissionDecision: Option[im.SubmissionDecision] =
    some(im.SubmissionDecision(Right(submissionAccept)))

  def submissionResponse: Option[im.SubmissionResponse] =
    some(im.SubmissionResponse(
      im.SubmissionReceipt(submissionId, System.currentTimeMillis(), None),
      submissionDecision,
      None
    ))

  def partnerLead = im.InvestigatorRef(pi)

  def ngoSubmission      = im.NgoSubmission(
    submissionRequest,
    submissionResponse,
    ngoPartner,
    partnerLead
  )
  def queueSubs: Either[List[im.NgoSubmission], im.ExchangeSubmission] =
    Left(List(ngoSubmission))

  def tooOption: im.ToOChoice = im.ToOChoice.None

  def proposalClass: im.ProposalClass = im.QueueProposalClass(
    None,
    None,
    proposalKey,
    queueSubs,
    None,
    tooOption,
    None,
    false
  )

  def proposal = im.Proposal(
    meta,
    semester,
    title,
    abs,
    "scheduling",
    tacCategory,
    investigators,
    targets,
    observations,
    proposalClass,
    "schemaVersion"
  )
}
