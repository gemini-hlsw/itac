package edu.gemini.tac.qservice.impl

import fixture.FixedQueue
import org.junit._
import Assert._

import edu.gemini.tac.persistence.{Committee => PsCommittee}
import edu.gemini.tac.persistence.{LogEntry => PsLogEntry}
import edu.gemini.tac.persistence.LogEntry.Type._
import edu.gemini.tac.persistence.{Proposal => PsProposal}
import edu.gemini.tac.persistence.queues.{Queue => PsQueue}
import edu.gemini.tac.persistence.queues.{ScienceBand => PsScienceBand}
import edu.gemini.tac.persistence.phase1.blueprint.{BlueprintBase => PsBlueprintBase}
import edu.gemini.tac.persistence.phase1.blueprint.niri.{NiriBlueprint => PsNiriBlueprint}

import queue.time.QueueTimeExtractor
import edu.gemini.tac.qengine.p1._

import edu.gemini.tac.persistence.fixtures.builders.{ProposalBuilder, QueueProposalBuilder}
import edu.gemini.tac.persistence.phase1.{TimeUnit, TimeAmount}
import edu.gemini.tac.qengine.ctx.Site
import edu.gemini.tac.service.{AbstractLogService => PsLogService}

import java.util.Date
import scala.collection.JavaConversions._
import edu.gemini.tac.qservice.impl.load.ProposalLoader
import edu.gemini.tac.psconversion.Partners

class TestQueueServiceImpl {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  private var q: PsQueue = null
  private var c: PsCommittee = null
  private val bps: java.util.List[PsBlueprintBase] = new java.util.ArrayList[PsBlueprintBase]()
  bps.add(new PsNiriBlueprint())

  @Before
  def init() {
    val tup = FixedQueue.mkQueue
    q = tup._1
    c = tup._2
  }

  private def fill(props: List[PsProposal]): MockLogService = {
    val c = new MockCommitteeService(props)
    c.saveCommitee(q.getCommittee)
    c.setPartnerSequenceProportional(q.getCommittee.getId)
    val l = new MockLogService
    QueueServiceImpl.fill(q, c, l)
    l
  }

  private def getEntries(t: PsLogEntry.Type, log: PsLogService): List[String] =
    log.getLogEntriesForQueue(q).filter(_.getTypes.contains(t)).map(_.getMessage).toList.reverse

  private def verifyLogContains(msg: String, log: PsLogService) {
    log.getLogEntriesForQueue(q).foreach { le => println(le.getMessage) }
    assertTrue(log.getLogEntriesForQueue(q).map(entry => entry.getMessage).exists(s => s.contains(msg)))
  }

  private def verifyContainsCalcTime(log: PsLogService) {
    getEntries(PROPOSAL_QUEUE_GENERATED, log) match {
      case h :: t => // ok
      case _ => fail("Missing calc time.")
    }
  }

//  private def printErrors(log: PsLogService) {
//    print(QUEUE_ERROR, log)
//  }
//
//  private def printSkip(log: PsLogService) {
//    print(PROPOSAL_SKIPPED, log)
//  }
//
//  private def print(t: PsLogEntry.Type, log: PsLogService) {
//    getEntries(t, log).foreach(println)
//  }

  @Test def testEmpty() {
    val l = fill(Nil)
    assertEquals(0, q.getBandings.size)
    assertTrue(getEntries(QUEUE_ERROR, l).isEmpty)
  }

  private def verifyBadQueue(msg: String) {
    val l = fill(Nil)
    assertEquals(0, q.getBandings.size)
    verifyLogContains(msg, l)
  }

  @Test def testBadQueueTime() {
    q.setTotalTimeAvailable(null)
    verifyBadQueue(QueueTimeExtractor.BAD_PARTNER_TIME)
  }

//  @Test def testBadQueueEngineConfig() {
//    val partner = PartnerFactory.mkPsPartner("XX")
//    q.setPartnerWithInitialPick(partner)
//    verifyBadQueue(ConfigExtractor.BAD_INITIAL_PARTNER(partner))
//  }

  // Multiple proposals with the same ID.
  @Test def testDuplicateIds() {
    ProposalBuilder.setDefaultCommittee(c)
    val prop1 = new QueueProposalBuilder().setCommittee(c).setPartner(FixedQueue.psPartners.find(_.getPartnerCountryKey == "US").get).setReceipt("US-1", new Date()).create()
    val prop2 = new QueueProposalBuilder().setCommittee(c).setPartner(FixedQueue.psPartners.find(_.getPartnerCountryKey == "US").get).setReceipt("US-2", new Date()).create()
    val prop3 = new QueueProposalBuilder().setCommittee(c).setPartner(FixedQueue.psPartners.find(_.getPartnerCountryKey == "US").get).setReceipt("US-1", new Date()).create()

    val props = List(prop1, prop2, prop3)
    val l = fill(props)

    verifyLogContains(ProposalLoader.DUPLICATE_IDS(Set(Proposal.Id(US, "US-1"))), l)
    verifyContainsCalcTime(l)
  }

  @Test def testAddOne() {
    val ps = new QueueProposalBuilder().setCommittee(c).create()
    val l = fill(List(ps))

    assertTrue(getEntries(QUEUE_ERROR, l).isEmpty)
    assertTrue(getEntries(PROPOSAL_SKIPPED, l).isEmpty)

    q.getBandings.toList match {
      case List(banding) => {
        assertEquals(PsScienceBand.BAND_ONE, banding.getBand)
        assertEquals(ps.getId, banding.getProposal.getId)
        assertEquals(0, banding.getMergeIndex)
      }
      case _ => fail()
    }

    // Check that available time was set.
    val avail = q.getAvailablePartnerTimes
    val partners = Partners.fromQueue(q).toOption.get

    partners.values foreach { p =>
      val pc = avail(partners.psPartnerFor(p))
      assertEquals(p.percentAt(Site.north), pc.getCharge.getDoubleValue, 0.000001)
    }

    // Check that there is no classical time
    val classical = q.getClassicalPartnerCharges
    partners.values foreach { p =>
      val pc = classical(partners.psPartnerFor(p))
      assertEquals(0.0, pc.getCharge.getDoubleValue, 0.000001)
    }

    verifyContainsCalcTime(l)
  }

  @Test def testSkipOne() {
    // Will make gs1 be skipped on a dec bin violation.
    val ps = new QueueProposalBuilder().
      setCommittee(c).
      setAccept(new TimeAmount(100, TimeUnit.HR), new TimeAmount(100, TimeUnit.HR), 1).
      addObservation("0:0:0.0", "90:0:0.0", new TimeAmount(100, TimeUnit.HR)).
      create()
    val l = fill(List(ps))

    assertTrue(getEntries(QUEUE_ERROR, l).isEmpty)

    q.getBandings.toList match {
      case Nil => // ok
      case _ => fail()
    }

    /* WRITING ALL ADD/SKIP DECISIONS IN A SINGLE LOG ENTRY FOR NOW.
    getEntries(PROPOSAL_SKIPPED, l) match {
      case List(b12, b3) => {
        assertTrue(b12.contains(prop.id.reference))
        assertTrue(b3.contains(prop.id.reference))
        assertTrue(b12.contains(QueueBand.Category.B1_2.toString))
        assertTrue(b3.contains(QueueBand.Category.B3.toString))
      }
      case _ => fail()
    }
    verifyContainsCalcTime(l)
    */

  }

  //  partners are allowed to run over as in this test case (the limit is 100)
  //  However overall we can't go over the max queue time.
  @Test def testOverallocation() {
    val ps = new QueueProposalBuilder().
      setCommittee(c).
      setAccept(new TimeAmount(101, TimeUnit.HR), new TimeAmount(101, TimeUnit.HR), 1).
      addObservation("1:0:0.0", "20:0:0.0", new TimeAmount(101, TimeUnit.HR)).
      create()
    val l = fill(List(ps))

    assertTrue(getEntries(QUEUE_ERROR, l).isEmpty)

    q.getBandings.toList match {
      case Nil => // ok
      case _ => fail()
    }

    /* WRITING ALL ADD/SKIP DECISIONS IN A SINGLE LOG ENTRY FOR NOW.
    getEntries(PROPOSAL_SKIPPED, l) match {
      case List(b12, b3) => {
        assertTrue(b12.contains(prop.id.reference))
        assertTrue(b3.contains(prop.id.reference))
        assertTrue(b12.contains(QueueBand.Category.B1_2.toString))
        assertTrue(b3.contains(QueueBand.Category.B3.toString))
      }
      case _ => fail()
    }
    verifyContainsCalcTime(l)
    */
  }

  @Test def testAddAndSkip() {
    ProposalBuilder.setDefaultCommittee(c)
    ProposalBuilder.setDefaultCondition(ProposalBuilder.WORST_CONDITIONS)
    // US -- add
    val psUS = new QueueProposalBuilder().
      setPartner(ProposalBuilder.dummyPartnerUS).
      setAccept(new TimeAmount(1, TimeUnit.HR), new TimeAmount(1, TimeUnit.HR), 1).
      addObservation("0:0:0.0", "20:0:0.0", new TimeAmount(1, TimeUnit.HR)).
      create()
    // GS -- skip.  Will make au1 be skipped on a dec bin violation.
    val psGS = new QueueProposalBuilder().
      setPartner(ProposalBuilder.dummyPartnerGS).
      setAccept(new TimeAmount(100, TimeUnit.HR), new TimeAmount(100, TimeUnit.HR), 1).
      addObservation("0:0:0.0", "90:0:0.0", new TimeAmount(1, TimeUnit.HR)).
      create()
    // BR -- add
    val psBR = new QueueProposalBuilder().
      setPartner(ProposalBuilder.dummyPartnerBR).
      setAccept(new TimeAmount(1, TimeUnit.HR), new TimeAmount(1, TimeUnit.HR), 1).
      addObservation("0:0:0.0", "20:0:0.0", new TimeAmount(1, TimeUnit.HR)).
      create()


    val l = fill(List(psUS, psGS, psBR))

    // No queue errors.
    assertTrue(getEntries(QUEUE_ERROR, l).isEmpty)

    /* WRITING ALL ADD/SKIP DECISIONS IN A SINGLE LOG ENTRY FOR NOW.

    // GS proposal skipped for being too long (twice)
    getEntries(PROPOSAL_SKIPPED, l) match {
      case List(b12, b3) => {
        assertTrue(b12.contains(propGS.id.reference))
        assertTrue(b3.contains(propGS.id.reference))
      }
      case _ => fail()
    }
    */

    q.getBandings.toList match {
      case List(banding1, banding2) => {
        assertEquals(PsScienceBand.BAND_ONE, banding1.getBand)
        assertEquals(0, banding1.getMergeIndex)
        assertEquals(PsScienceBand.BAND_ONE, banding2.getBand)
        assertEquals(1, banding2.getMergeIndex)
      }
      case _ => fail()
    }

    def qContains(prop: PsProposal): Boolean =
      q.getBandings.toList.exists(_.getProposal.getPhaseIProposal.getReceiptId.equals(prop.getPhaseIProposal.getReceiptId))

    assertTrue(qContains(psUS))
    assertTrue(qContains(psBR))
    assertFalse(qContains(psGS))
    verifyContainsCalcTime(l)
  }

  @Test def testPoorWeather() {
    ProposalBuilder.setDefaultCommittee(c)
    ProposalBuilder.setDefaultCondition(ProposalBuilder.WORST_CONDITIONS)
    // US -- add
    val psUS = new QueueProposalBuilder().
      //      setCommittee(c).
      setPartner(ProposalBuilder.dummyPartnerUS).
      setAccept(new TimeAmount(1, TimeUnit.HR), new TimeAmount(1, TimeUnit.HR), 1).
      addObservation("0:0:0.0", "20:0:0.0", new TimeAmount(1, TimeUnit.HR)).
      create()
    //    // GS -- skip.  Will make gs1 be skipped on a dec bin violation.
    val psCA = new QueueProposalBuilder().
      //      setCommittee(c).
      setPartner(ProposalBuilder.dummyPartnerCA).
      setAccept(new TimeAmount(100, TimeUnit.HR), new TimeAmount(100, TimeUnit.HR), 1, /*poor weather:*/ true).
      //      setItacAccept(new TimeAmount(100, TimeUnit.HR)).
      addObservation("0:0:0.0", "90:0:0.0", new TimeAmount(1, TimeUnit.HR)).
      create()
    // BR -- add
    val psBR = new QueueProposalBuilder().
      //      setCommittee(c).
      setPartner(ProposalBuilder.dummyPartnerBR).
      setAccept(new TimeAmount(1, TimeUnit.HR), new TimeAmount(1, TimeUnit.HR), 1).
      addObservation("0:0:0.0", "20:0:0.0", new TimeAmount(1, TimeUnit.HR)).
      create()


    val props = List(psUS, psCA, psBR)
    //confirm 1 is poor weather
    assertEquals(1, props.count(_.isPw))
    val l = fill(props)

    // No queue errors.
    assertTrue(getEntries(QUEUE_ERROR, l).isEmpty)

    println(q.getBandings)
    q.getBandings.toList match {
      case List(banding1, banding2, banding3) =>
        assertEquals(PsScienceBand.BAND_ONE, banding1.getBand)
        assertEquals(0, banding1.getMergeIndex)
        assertEquals(PsScienceBand.BAND_ONE, banding2.getBand)
        assertEquals(1, banding2.getMergeIndex)
        assertEquals(PsScienceBand.POOR_WEATHER, banding3.getBand)
        assertEquals(2, banding3.getMergeIndex)
        assertEquals("CA", banding3.getPartnerAbbreviation)
      case bs =>
        println(bs)
        fail()
    }

    def qContains(prop: PsProposal): Boolean =
      q.getBandings.toList.exists(_.getProposal.getPhaseIProposal.getReceiptId.equals(prop.getPhaseIProposal.getReceiptId))

    assertTrue(qContains(psUS))
    assertTrue(qContains(psBR))
    assertTrue(qContains(psCA))
    verifyContainsCalcTime(l)
  }
}