package edu.gemini.tac.psconversion.test

import edu.gemini.tac.persistence.{Partner => PsPartner, Site => PsSite}
import edu.gemini.tac.service.{IPartnerService => PsPartnerService, HibernateSource}
import edu.gemini.tac.qengine.ctx.{Site, Partner}
import scala.collection.JavaConverters._

private object Complete extends PsPartnerService {

  val partners: List[(Partner, PsPartner)] =
    HibernateSource.partners.map { ps =>
      val id = ps.getPartnerCountryKey
      val name = ps.getName
      val perc = ps.getPercentageShare
      val sites = ps.getSites.asScala.map { s => if (s == PsSite.NORTH) Site.north else Site.south }.toSet
      (Partner(id, name, perc * 100.0, sites), ps)
    }

  def findAllPartners: java.util.List[PsPartner] = partners.unzip._2.asJava

  def findAllPartnerCountries: java.util.List[PsPartner] = {
    val initials = Set("AR", "AU", "BR", "CA", "CL", "US")
    partners.unzip._2.filterNot(p => initials.contains(p.getPartnerCountryKey)).asJava
  }

  def findForKey(key: String): PsPartner = findAllPartnerCountries.asScala.filter(_.getPartnerCountryKey.equals(key)).head

  def getPartnersByName: java.util.Map[String, PsPartner] = {
    val map = new java.util.HashMap[String, PsPartner]()
    for (partner: PsPartner <- findAllPartnerCountries.asScala) {
      map.put(partner.getName, partner)
    }
    map
  }

  def setNgoContactEmail(partnerName: String, email: String) = {
    findAllPartners.asScala.filter(_.getName == partnerName).head.setNgoFeedbackEmail(email)
  }

  override def setPartnerPercentage(partnerName: String, pct: Double) = {
    findAllPartners.asScala.filter(_.getName == partnerName).head.setPercentageShare(pct)
  }
}

private object Incomplete extends PsPartnerService {
  def findAllPartners: java.util.List[PsPartner] = List(Complete.partners.head._2).asJava

  def findAllPartnerCountries = null

  def findForKey(key: String) = null

  def getPartnersByName = null

  def setNgoContactEmail(partnerName: String, email: String) {}

  override def setPartnerPercentage(partnerName: String, pct: Double) {
    findAllPartners.asScala.filter(_.getName == partnerName).head.setPercentageShare(pct)
  }
}

private object Empty extends PsPartnerService {
  def findAllPartners = new java.util.ArrayList[PsPartner]

  def findAllPartnerCountries = null

  def findForKey(key: String) = null

  def getPartnersByName = null

  def setNgoContactEmail(partnerName: String, email: String) {}

  override def setPartnerPercentage(partnerName: String, pct: Double) {
    findAllPartners.asScala.filter(_.getName == partnerName).head.setPercentageShare(pct)
  }
}

private object Null extends PsPartnerService {
  def findAllPartners = null

  def findAllPartnerCountries = null

  def findForKey(key: String) = null

  def getPartnersByName = null

  def setNgoContactEmail(partnerName: String, email: String) {}

  override def setPartnerPercentage(partnerName: String, pct: Double) {}
}

object MockPartnerService {
  def complete: PsPartnerService = Complete

  def incomplete: PsPartnerService = Incomplete

  def nulo: PsPartnerService = Null

  def empty: PsPartnerService = Empty
}
