package edu.gemini.tac.psconversion

import edu.gemini.tac.{persistence => ps}
import edu.gemini.tac.qengine.ctx.{Site, Partner}

import scala.collection.JavaConverters._
import scalaz._
import Scalaz._

/**
 *
 */
object PartnerConverter {
  def MISSING_PARTNER_COUNTRY_KEY: String = "Missing parnter country key"
  def UNRECOGNIZED_PARTNER(id: String, partners: List[Partner]): String =
    s"Partner $id not in valid partner list: ${partners.map(_.id).sorted.mkString(",")}"


  protected def sitesFor(psSites: java.util.Set[ps.Site]): PsError \/ Set[Site] = {
    val sites = psSites.asScala.toSet[ps.Site].map { psSite =>
      if (psSite == ps.Site.NORTH) Site.north else Site.south
    }
    if (sites.size == 0) BadData("No partner sites specified").left
    else sites.right
  }

  def read(psPartner: ps.Partner): PsError \/ Partner =
    for {
      id      <- nullSafePersistence("Missing partner abbreviation")     { psPartner.getPartnerCountryKey }
      name    <- nullSafePersistence("Missing partner name")             { psPartner.getName }
      perc    <- nullSafePersistence("Missing partner share percentage") { psPartner.getPercentageShare }
      psSites <- nullSafePersistence("Missing partner sites")            { psPartner.getSites }
      sites   <- sitesFor(psSites)
    } yield Partner(id, name, perc * 100.0, sites)

  def find(psPartner: ps.Partner, partners: List[Partner]): PsError \/ Partner =
    for {
      abbr    <- nullSafePersistence(MISSING_PARTNER_COUNTRY_KEY) { psPartner.getPartnerCountryKey }
      partner <- partners.find(_.id == abbr) \/> BadData(UNRECOGNIZED_PARTNER(abbr, partners))
    } yield partner
}
