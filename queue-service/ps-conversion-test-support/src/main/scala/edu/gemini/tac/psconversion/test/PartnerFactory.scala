package edu.gemini.tac.psconversion.test

import edu.gemini.tac.persistence.{Partner => PsPartner, Site => PsSite}

import scala.collection.JavaConverters._

object PartnerFactory {
  def mkPsPartner(abbr: String): PsPartner =
    new PsPartner {
      override def getAbbreviation: String = abbr
      override def getPartnerCountryKey : String = abbr
      override def getName: String = abbr
      override def toString: String = "PsPartner(" + abbr + ")"
      override def isNorth : Boolean = true
      override def isSouth : Boolean = true
      override def equals(o: Any): Boolean =
        o match {
          case that: PsPartner => getAbbreviation == that.getAbbreviation
          case _ => false
        }
      override def getSites: java.util.Set[PsSite] = PsSite.ALL_SET


      override def hashCode: Int = getAbbreviation.hashCode
    }

  def mkPsPartners(abbrs: String*): java.util.Set[PsPartner] =
    abbrs.map(mkPsPartner).toSet.asJava
}