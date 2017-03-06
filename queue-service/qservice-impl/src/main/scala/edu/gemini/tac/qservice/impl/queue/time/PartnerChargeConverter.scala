package edu.gemini.tac.qservice.impl.queue.time

import edu.gemini.tac.persistence.{Partner => PsPartner}
import edu.gemini.tac.persistence.queues.partnerCharges.{PartnerCharge => PsPartnerCharge}
import edu.gemini.tac.psconversion._
import edu.gemini.tac.qengine.api.queue.time.PartnerTime
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.util.Time

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/**
* Extracts partner time information from the persistence PartnerCharge.
*/
class PartnerChargeConverter[T <: PsPartnerCharge](name: String, jmap: java.util.Map[PsPartner, T], partners: List[Partner]) {

  private def extract(pc: PsPartnerCharge): PsError \/ (Partner, Time) =
    for {
      psPartner   <- nullSafePersistence(MISSING_PARTNER) { pc.getPartner }
      partnerCode <- nullSafePersistence("Unable to get partner country code") { psPartner.getPartnerCountryKey }
      partner     <- partners.find(_.id == partnerCode) \/> BadData(UNRECOGNIZED_PARTNER(psPartner))
      psTime      <- nullSafePersistence(MISSING_TIME) { pc.getCharge }
      time        <- TimeConverter.toTime(psTime)
    } yield partner -> time

  private def toPartnerChargeList: PsError \/ List[T] =
    for {
      m <- Option(jmap) \/> BadData(MISSING_CONFIG)
    } yield m.values.asScala.toList

  private def toPartnerAndTime: PsError \/ List[(Partner, Time)] =
    toPartnerChargeList.flatMap { _.map(extract).sequenceU }

  def partnerTime: PsError \/ PartnerTime =
    toPartnerAndTime.map { lst => PartnerTime(partners, lst.toMap) }

  def MISSING_CONFIG  = "Missing %s partner charge configuration".format(name)
  def MISSING_PARTNER = "%s partner charge missing partner".format(name)
  def UNRECOGNIZED_PARTNER(psPartner: PsPartner) =
    "%s partner charge contained unrecognized partner '%s'".format(name, psPartner.getAbbreviation)
  def MISSING_TIME    = "%s partner charge missing time charge".format(name)
}