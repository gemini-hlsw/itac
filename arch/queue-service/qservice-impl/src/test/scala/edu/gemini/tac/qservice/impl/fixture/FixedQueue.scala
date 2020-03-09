package edu.gemini.tac.qservice.impl.fixture

import edu.gemini.tac.persistence.{Committee => PsCommittee}
import edu.gemini.tac.persistence.{Partner => PsPartner}
import edu.gemini.tac.persistence.{Semester => PsSemester}
import edu.gemini.tac.persistence.{Site => PsSite}
import edu.gemini.tac.persistence.bandrestriction.{BandRestrictionRule => PsBandRestrictionRule}
import edu.gemini.tac.persistence.bandrestriction._
import edu.gemini.tac.persistence.bin.{BinConfiguration => PsBinConfiguration}
import edu.gemini.tac.persistence.bin.{DecBinSize => PsDecBinSize}
import edu.gemini.tac.persistence.bin.{RABinSize => PsRaBinSize}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.persistence.restrictedbin.{RestrictedBin => PsRestrictedBin}
import edu.gemini.tac.persistence.restrictedbin._
import edu.gemini.tac.persistence.phase1.queues.PartnerPercentage

import edu.gemini.tac.qservice.impl.p1.PsFixture

import scala.collection.JavaConverters._

object FixedQueue {
  val psPartners = PsFixture.psPartners

  def mkInteger(int: Int): java.lang.Integer = new java.lang.Integer(int)

  def mkQueue: (PsQueue, PsCommittee) = {
    val semester = new PsSemester {
      override def getName = "B"
      override def getYear = 2012
      override def getDisplayName = "2012B"
    }
    val c = new PsCommittee {
      override def getSemester = semester
    }
    c.setId(1)
    val q = new PsQueue("Test Queue Name", c)

    // QueueTime
    q.setTotalTimeAvailable(mkInteger(100))
    q.setBand1Cutoff(mkInteger(30))
    q.setBand2Cutoff(mkInteger(60))

    // Partner Sequence
    val partner = psPartners.find(_.getPartnerCountryKey == "BR").get
    q.setPartnerWithInitialPick(partner)

    // Restricted Bins
    val restricted = new java.util.HashSet[PsRestrictedBin]
    val wv = new WaterVaporPercentageRestrictedBin {
      override def getValue: java.lang.Integer = mkInteger(50)
    }
    val lgs = new LgsObservationsRestrictedBin {
      override def getValue: java.lang.Integer = mkInteger(200)
    }
    restricted.add(wv)
    restricted.add(lgs)
    q.setRestrictedBins(restricted)

    // Band Restrictions
    val rules = new java.util.HashSet[PsBandRestrictionRule]
    rules.add(new Iq20RestrictionNotInBand3)
    rules.add(new LgsRestrictionInBandsOneAndTwo)
    rules.add(new RapidTooRestrictionInBandOne)
    q.setBandRestrictionRules(rules)

    // SiteSemester (RA/Dec)
    val site = PsSite.NORTH
    q.setSite(site)


    val raBinSize = new PsRaBinSize
    raBinSize.setHours(mkInteger(2))
    raBinSize.setMinutes(mkInteger(0))

    val decBinSize = new PsDecBinSize
    decBinSize.setDegrees(mkInteger(20))

    val binConfig = new PsBinConfiguration
    binConfig.setRaBinSize(raBinSize)
    binConfig.setDecBinSize(decBinSize)
    q.setBinConfiguration(binConfig)

    //Partner Percentages
    val pps = psPartners.map { p: PsPartner =>
      new PartnerPercentage(q, p, p.getPercentageShare(PsSite.NORTH))
    }.toSet.asJava

    q.setPartnerPercentages(pps)

    q.setSubaruScheduledByQueueEngine(false)

    (q, c)
  }
}