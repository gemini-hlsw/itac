package edu.gemini.tac.service.check.impl.checks

import edu.gemini.tac.service.{check => api}
import edu.gemini.tac.service.check.impl.core.StatelessProposalCheckFunction
import edu.gemini.tac.service.check.impl.core.ProposalIssue._

import edu.gemini.tac.persistence.RichProposal._
import edu.gemini.tac.persistence.phase1._
import blueprint.BlueprintBase
import edu.gemini.tac.persistence.phase1.sitequality._

import CloudCover.{CC_70 => CC70, CC_100 => CCAny}
import ImageQuality.{IQ_70 => IQ70, IQ_100 => IQAny}
import SkyBackground.{SB_80 => SB80, SB_100 => SBAny}
import WaterVapor.{WV_80 => WV80, WV_100 => WVAny}
import edu.gemini.tac.persistence.{ProposalIssueCategory, Proposal}
import edu.gemini.model.p1.mutable.TooOption

object ConditionsCheck extends StatelessProposalCheckFunction {
  val name = "Conditions"

  def description = tests.map(_.description).mkString(" ")

  case class MiniObs(proposal: Proposal, siteQuality: Condition, blueprint: BlueprintBase) {
    def cc: CloudCover = siteQuality.getCloudCover

    def iq: ImageQuality = siteQuality.getImageQuality

    def sb: SkyBackground = siteQuality.getSkyBackground

    def wv: WaterVapor = siteQuality.getWaterVapor

    def nici: Boolean = Instrument.NICI == blueprint.getInstrument

    def gmos: Boolean = Instrument.GMOS_N == blueprint.getInstrument || Instrument.GMOS_S == blueprint.getInstrument

    def trecs: Boolean = Instrument.TRECS == blueprint.getInstrument

    def michelle: Boolean = Instrument.MICHELLE == blueprint.getInstrument

    def gsaoi: Boolean = Instrument.GSAOI == blueprint.getInstrument

    def lgs: Boolean = blueprint.hasLgs

    def altair: Boolean = blueprint.hasAltair

    def mcao: Boolean = blueprint.hasMcao

    def hasAO: Boolean = blueprint.hasAO
  }

  private def toMini(p: Proposal, o: Observation): MiniObs =
    MiniObs(p, o.getCondition, o.getBlueprint)

  private def obsList(p: Proposal): List[MiniObs] =
    p.observations map {
      obs => toMini(p, obs)
    }

  trait ConditionsRule {
    val description: String

    def appliesTo(obs: MiniObs): Boolean

    def meetsConditionsRequirement(obs: MiniObs): Boolean

    def detailMessage(obs: MiniObs): String

    def issue(p: Proposal, bigObs: Observation): Option[api.ProposalIssue] =
      issue(p, toMini(p, bigObs))

    def issue(p: Proposal, o: MiniObs): Option[api.ProposalIssue] =
      if (appliesTo(o) && !meetsConditionsRequirement(o))
        Some(error(ConditionsCheck, p, detailMessage(o), ProposalIssueCategory.ObservingConstraint))
      else
        None
  }

  // ITAC-620
  object GsaoiCloudCoverConstraintTest extends ConditionsRule {
    val description = "GSAOI observations must not have CloudCover 70, 80 or Any."

    def appliesTo(obs: MiniObs) = obs.gsaoi

    def meetsConditionsRequirement(obs: MiniObs) = {
      val observationCloudCover = obs.cc.getPercentage
      val maximumCloudCover = CloudCover.CC_70.getPercentage
      observationCloudCover < maximumCloudCover
    }

    def detailMessage(cc : CloudCover) = "GSAOI observation with Cloud Cover %s".format(cc.getDisplayName)

    def detailMessage(obs: MiniObs) = detailMessage(obs.cc)
  }

  // UX-1344 ITAC-470
  object AoNotIqAnyTest extends ConditionsRule {
    val description =
      "Observations that make use of an add-on AO component or that use GSAOI must not use Observing " +
        "Conditions with IQAny."

    def appliesTo(obs: MiniObs): Boolean = obs.hasAO || obs.gsaoi

    def meetsIq(obs: MiniObs): Boolean = (obs.iq.getPercentage != IQAny.getPercentage)

    def meetsConditionsRequirement(obs: MiniObs): Boolean =
      meetsIq(obs)

    def detailMessage(obs: MiniObs): String = {
      val aoComponent = if (obs.altair) "Altair" else "MCAO"
      "%s observation with IQAny.".format(aoComponent)
    }
  }

  // ITAC-468, UX-1345
  object NotAllConditionsAnyTest extends ConditionsRule {

    val description =
      "Observations that are not rapid ToOs must not have all Observing " +
        "Conditions set to Any."

    def appliesTo(obs: MiniObs): Boolean = obs.proposal.getPhaseIProposal.getTooOption != TooOption.RAPID

    def meetsConditionsRequirement(obs: MiniObs): Boolean =
      !((obs.iq.getPercentage == IQAny.getPercentage) &&
        (obs.cc.getPercentage == CCAny.getPercentage) &&
        (obs.sb.getPercentage == SBAny.getPercentage) &&
        (obs.wv.getPercentage == WVAny.getPercentage))

    def detailMessage(obs: MiniObs): String =
      "Observation with all Observing Conditions set to Any (only allowed for Rapid-ToOs."
  }

  object IqTest extends ConditionsRule {
    val description =
      "Observations that make use of LGS, MCAO, or NICI must use Observing " +
        "Conditions that specify IQ20 or IQ70 with CC50."

    def appliesTo(obs: MiniObs): Boolean = obs.lgs || obs.nici

    def meetsLgsCc(obs: MiniObs): Boolean = (obs.cc.getPercentage < CC70.getPercentage)

    def meetsNiciCc(obs: MiniObs): Boolean = (obs.cc.getPercentage <= CC70.getPercentage)

    def meetsIq(obs: MiniObs): Boolean = (obs.iq.getPercentage <= IQ70.getPercentage)

    def meetsCc(obs: MiniObs): Boolean =
      (obs.lgs && meetsLgsCc(obs)) || (obs.nici && meetsNiciCc(obs))

    def meetsConditionsRequirement(obs: MiniObs): Boolean =
      meetsIq(obs) && meetsCc(obs)

    def detailMessage(isLGS: Boolean, cc: CloudCover, iq: ImageQuality): String = {
      val uses = if (isLGS) "LGS" else "NICI"
      val (kind, value) = if (iq.getPercentage > IQ70.getPercentage) ("image quality", iq.getDisplayName) else ("cloud cover", cc.getDisplayName)
      "%s observation with bad %s (%s).".format(uses, kind, value)
    }

    def detailMessage(obs: MiniObs) = detailMessage(obs.lgs, obs.cc, obs.iq)
  }

  object GmosSbTest extends ConditionsRule {
    val description =
      "Observations using GMOS must require observing conditions with SB 20, " +
        "50, or 80 and WV 80 or Any."

    def appliesTo(obs: MiniObs): Boolean = obs.gmos

    def meetsConditionsRequirement(obs: MiniObs): Boolean =
      (obs.sb.getPercentage < SBAny.getPercentage) && (obs.wv.getPercentage >= WV80.getPercentage)

    def detailMessage(sb: SkyBackground, wv: WaterVapor): String = {
      val why = if (sb.getPercentage == SBAny.getPercentage) "SBAny" else "good WV (%s)".format(wv.getDisplayName)
      "GMOS observation with %s.".format(why)
    }

    def detailMessage(obs: MiniObs) = detailMessage(obs.sb, obs.wv)
  }

  object NonGmosSbTest extends ConditionsRule {
    val description = "Non-GMOS observations must require SB80 or SBAny."

    def appliesTo(obs: MiniObs): Boolean = !obs.gmos

    def meetsConditionsRequirement(obs: MiniObs): Boolean = obs.sb.getPercentage >= SB80.getPercentage

    def detailMessage(inst: String, sb: SkyBackground): String =
      "Non-GMOS observation (%s) with good sky background (%s).".format(inst, sb.getDisplayName)

    def detailMessage(obs: MiniObs) = detailMessage(obs.blueprint.getInstrument.getDisplayName, obs.sb)
  }

  object WvTest extends ConditionsRule {
    val description =
      "Observations using T-ReCS or Michelle must require observing conditions " +
        "with WV20, 50, or 80."

    def appliesTo(obs: MiniObs): Boolean = obs.trecs || obs.michelle

    def meetsConditionsRequirement(obs: MiniObs): Boolean = obs.wv.getPercentage < WVAny.getPercentage

    def detailMessage(inst: String) = "%s observation with WVAny.".format(inst)

    def detailMessage(obs: MiniObs) = detailMessage(obs.blueprint.getInstrument.getDisplayName)
  }

  val tests = List(IqTest, AoNotIqAnyTest, NotAllConditionsAnyTest, GmosSbTest, NonGmosSbTest, WvTest, GsaoiCloudCoverConstraintTest)

  def apply(p: Proposal): Set[api.ProposalIssue] =
    (noIssues /: obsList(p)) {
      (issues, miniObs) => (issues /: tests) {
        (issues, test) => test.issue(p, miniObs) map {
          newIssue => issues + newIssue
        } getOrElse issues
      }
    }
}