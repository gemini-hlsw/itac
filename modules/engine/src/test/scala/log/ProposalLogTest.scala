package edu.gemini.tac.qengine.log

import org.junit._
import Assert._
import edu.gemini.tac.qengine.ctx.{TestPartners, Partner}
import edu.gemini.tac.qengine.util.{BoundedTime, Time}
import ProposalLog.Key
import ProposalLog.Entry
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.p1.QueueBand.Category._
import edu.gemini.spModel.core.Site

class ProposalLogTest {
  import TestPartners._
  val partners = All

  private val dummyProp: Proposal =
    CoreProposal(Ntac(AR, "x", 0, Time.Zero), Site.GS)

  private def propId(p: Partner, s: String): Proposal.Id = new Proposal.Id(p, s)
  private def br(i: Int): Proposal.Id                    = propId(BR, i.toString)
  private def ca(i: Int): Proposal.Id                    = propId(CA, i.toString)
  private def us(i: Int): Proposal.Id                    = propId(US, i.toString)

  @Test def testEmptyProposalLog() {
    val log = ProposalLog.Empty

    try {
      log(us(0), B1_2)
      fail("not defined")
    } catch {
      case ex: Exception => // ok
    }

    try {
      log(Key(us(0), B1_2))
      fail("not defined")
    } catch {
      case ex: Exception => // ok
    }

    assertEquals(None, log.get(us(0), B1_2))
    assertEquals(None, log.get(Key(us(0), B1_2)))

    val msg = RejectNoObs(dummyProp)
    assertEquals(msg, log.getOrElse(us(0), B1_2, msg))
    assertEquals(msg, log.getOrElse(Key(us(0), B1_2), msg))

    assertEquals(Nil, log.toList(us(0)))
    assertEquals(Nil, log.toList)
    assertEquals(Nil, log.toDetailList)

    assertEquals(Set.empty, log.proposalIds)
  }

  @Test def testSingleKey() {
    val msg = RejectNoObs(dummyProp)
    val log = ProposalLog((us(0), B1_2, msg))

    assertEquals(msg, log(us(0), B1_2))
    assertEquals(msg, log(Key(us(0), B1_2)))
    assertEquals(Some(msg), log.get(us(0), B1_2))
    val exp = List(Entry(Key(us(0), B1_2), msg))
    assertEquals(exp, log.toList(us(0)))
    assertEquals(exp, log.toList)
    assertEquals(exp, log.toDetailList)
    assertEquals(Set(us(0)), log.proposalIds)

    assertTrue(log.isDefinedAt(Key(us(0), B1_2)))
    assertTrue(log.isDefinedAt(us(0), B1_2))
    assertFalse(log.isDefinedAt(Key(us(1), B1_2)))
    assertFalse(log.isDefinedAt(us(1), B1_2))

    assertEquals(Some(msg), log.get(us(0), B1_2))
    assertEquals(None, log.get(us(1), B1_2))
  }

  @Test def testTwoCatOneId() {
    val msg1 = RejectNoObs(dummyProp)
    val msg2 = RejectNotBand3(dummyProp)

    val log = ProposalLog((us(0), B1_2, msg1), (us(0), B3, msg2))

    assertEquals(msg1, log(us(0), B1_2))
    assertEquals(msg2, log(us(0), B3))
    val exp = List(Entry(Key(us(0), B1_2), msg1), Entry(Key(us(0), B3), msg2))
    assertEquals(exp, log.toList(us(0)))
    assertEquals(exp, log.toList)
    assertEquals(exp, log.toDetailList)
    assertEquals(Set(us(0)), log.proposalIds)
  }

  @Test def testTwoId() {
    val msg1 = RejectNoObs(dummyProp)
    val msg2 = RejectNotBand3(dummyProp)

    val log = ProposalLog((us(0), B1_2, msg1), (br(0), B3, msg2))

    assertEquals(msg1, log(us(0), B1_2))
    assertEquals(msg2, log(br(0), B3))

    val usEntry = Entry(Key(us(0), B1_2), msg1)
    val brEntry = Entry(Key(br(0), B3), msg2)
    assertEquals(List(usEntry), log.toList(us(0)))
    assertEquals(List(brEntry), log.toList(br(0)))
    assertEquals(List(usEntry, brEntry), log.toList)
    assertEquals(List(usEntry, brEntry), log.toDetailList)

    assertEquals(Set(us(0), br(0)), log.proposalIds)
  }

  @Test def testFilterDups() {
    val bt   = BoundedTime(Time.hours(1.0))
    val msg1 = AcceptMessage(dummyProp, bt, bt)
    val msg2 = RejectNoObs(dummyProp)
    val msg3 = RejectNotBand3(dummyProp)

    val tups = List(
      (us(0), B3, msg1),
      (us(0), B3, msg2),
      (us(0), B3, msg3)
    )
    val log = ProposalLog(tups: _*)

    assertEquals(msg3, log(us(0), B3))

    val lst = List(Entry(Key(us(0), B3), msg3))
    assertEquals(lst, log.toList)
    assertEquals(lst, log.toList(us(0)))

    val detailList = List(
      Entry(Key(us(0), B3), msg1),
      Entry(Key(us(0), B3), msg2),
      Entry(Key(us(0), B3), msg3)
    )
    assertEquals(detailList, log.toDetailList)
  }

  @Test def testKeyOrdering() {
    val cat  = QueueBand.Category.B1_2
    val msg1 = RejectNoObs(dummyProp)
    val msg2 = RejectNotBand3(dummyProp)
    val msg3 =
      RejectPartnerOverAllocation(dummyProp, BoundedTime(Time.Zero), BoundedTime(Time.Zero))
    val msg4 = RejectCategoryOverAllocation(dummyProp, cat)

    val log = ProposalLog.Empty

    val id1 = br(1)
    val id2 = us(0)
    val id3 = br(0)
    val id4 = ca(0)

    val log1 = log.updated(id1, cat, msg1)
    val log2 = log1.updated(id2, cat, msg2)
    val log3 = log2.updated(id3, cat, msg3)
    val log4 = log3.updated(id4, cat, msg4)

    // toList gives you insertion order
    val idList = log4.toList.map { entry =>
      entry.key.id
    }
    assertEquals(List(id1, id2, id3, id4), idList)

    // Normal toList is key ordered
    val idList2 = log4.toList.sorted(ProposalLog.KeyOrdering).map { entry =>
      entry.key.id
    }
    assertEquals(List(id3, id1, id4, id2), idList2)
  }

  @Test def testToMap() {
    val cat12 = QueueBand.Category.B1_2
    val cat3  = QueueBand.Category.B3
    val msg1  = RejectNoObs(dummyProp)
    val msg2  = RejectNotBand3(dummyProp)
    val msg3 =
      RejectPartnerOverAllocation(dummyProp, BoundedTime(Time.Zero), BoundedTime(Time.Zero))
    val msg4 = RejectCategoryOverAllocation(dummyProp, cat12)

    val log = ProposalLog.Empty
    assertEquals(Map.empty, log.toMap)

    val br0 = br(0)
    val us0 = us(0)

    val log1 = log.updated(br0, cat12, msg1)
    val log2 = log1.updated(us0, cat12, msg2)
    val log3 = log2.updated(br0, cat3, msg3)
    val log4 = log3.updated(br0, cat12, msg4)

    val map = Map(Key(br0, cat12) -> msg4, Key(br0, cat3) -> msg3, Key(us0, cat12) -> msg2)
    assertEquals(map, log4.toMap)
  }

  @Test def testUpdatedFunction() {
    val cat = QueueBand.Category.B1_2
    val ar0 = CoreProposal(Ntac(AR, "ar0", 0, Time.Zero), Site.GS)
    val ar1 = CoreProposal(Ntac(AR, "ar1", 0, Time.Zero), Site.GS)
    val br0 = CoreProposal(Ntac(BR, "br0", 0, Time.Zero), Site.GS)

    val propList = List(ar0, ar1, br0)

    val log = ProposalLog.Empty
    assertEquals(List.empty, log.updated(List(), cat, RejectCategoryOverAllocation(_, cat)).toList)

    val log2 = log.updated(propList, cat, RejectCategoryOverAllocation(_, cat))

    log2.toList match {
      case List(e0, e1, e2) =>
        assertEquals(Entry(Key(ar0.id, cat), RejectCategoryOverAllocation(ar0, cat)), e0)
        assertEquals(Entry(Key(ar1.id, cat), RejectCategoryOverAllocation(ar1, cat)), e1)
        assertEquals(Entry(Key(br0.id, cat), RejectCategoryOverAllocation(br0, cat)), e2)
      case _ => fail()
    }
  }

  @Test def producesXml() {
    val cat = QueueBand.Category.B1_2
    val ar0 = CoreProposal(Ntac(AR, "ar0", 0, Time.Zero), Site.GS)
    val ar1 = CoreProposal(Ntac(AR, "ar1", 0, Time.Zero), Site.GS)
    val br0 = CoreProposal(Ntac(BR, "br0", 0, Time.Zero), Site.GS)

    val propList = List(ar0, ar1, br0)

    val log = ProposalLog.Empty
    assertEquals(List.empty, log.updated(List(), cat, RejectCategoryOverAllocation(_, cat)).toList)

    val log2 = log.updated(propList, cat, RejectCategoryOverAllocation(_, cat))
    val xml  = log2.toXML
    assertNotNull(xml)

  }
}
