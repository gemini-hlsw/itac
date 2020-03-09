package edu.gemini.tac.qengine.impl.resource

import org.junit._
import Assert._
import edu.gemini.tac.qengine.p1.CloudCover.CCAny
import edu.gemini.tac.qengine.p1.ImageQuality._
import edu.gemini.tac.qengine.p1.SkyBackground.SBAny
import edu.gemini.tac.qengine.p1.WaterVapor.WVAny
import edu.gemini.tac.qengine.impl.block.Block
import edu.gemini.tac.qengine.api.config.Default
import edu.gemini.tac.qengine.api.queue.time.QueueTime
import edu.gemini.tac.qengine.util.{Percent, Time}
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder
import edu.gemini.tac.qengine.log.{ProposalLog, RejectBand}
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.Site

class BandResourceTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  private val ntac   = Ntac(US, "x", 0, Time.hours(10))
  private val target = Target(0.0, 0.0) // not used

  private def conds(iq: ImageQuality, cc: CloudCover = CCAny) =
    ObservingConditions(cc, iq, SBAny, WVAny)

  private def mkProp(time: Time): CoreProposal =
    CoreProposal(
      ntac.copy(awardedTime = time),
      site = Site.GS,
      obsList = List(Observation(target, conds(IQAny), time))
    )

  private def mkProp(iq: ImageQuality, b3IQ: Option[ImageQuality]): CoreProposal = {
    val b3 = List.empty
    CoreProposal(
      ntac,
      site = Site.GS,
      band3Observations = b3,
      obsList = List(Observation(target, conds(iq), Time.hours(10)))
    )
  }

  private def mkProp(too: Too.Value): CoreProposal =
    CoreProposal(
      ntac,
      site = Site.GS,
      too = too,
      obsList = List(Observation(target, conds(IQAny), Time.hours(10)))
    )

  private def mkProp(lgs: Boolean): CoreProposal =
    CoreProposal(
      ntac,
      site = Site.GS,
      obsList = List(Observation(target, conds(IQAny), Time.hours(10), lgs))
    )

  val br                          = new BandResource(Default.BandRestrictions)
  val timeMap: Map[Partner, Time] = Map(US -> Time.hours(100))
  val queue = ProposalQueueBuilder(
    QueueTime(Site.GS, timeMap, partners),
    ProposalQueueBuilder.DefaultStrategy
  )

  // Band 1: ( 0, 30]
  // Band 2: (30, 60]
  // Band 3: (60, 80]

  @Test def testMiddleBlocksSkipped() {
    val queue2 = queue :+ mkProp(Time.hours(60.1))
    assertEquals(QueueBand.QBand3, queue2.band)

    val prop = mkProp(lgs = true)
    // not start or final block
    val block = Block(prop, prop.obsList.head, Time.hours(1), isStart = false, isFinal = false)

    // Would fail for being an LGS program in band 3
    br.reserve(block, queue2) match {
      case Right(res) => assertSame(br, res)
      case _          => fail()
    }
  }

  @Test def testLastBlockChecked() {
    val queue2 = queue :+ mkProp(Time.hours(60.1))
    assertEquals(QueueBand.QBand3, queue2.band)

    val prop = mkProp(lgs = true)
    // a final block
    val block = Block(prop, prop.obsList.head, Time.hours(1), isStart = false, isFinal = true)

    // Fails for being an LGS program in band 3
    br.reserve(block, queue2) match {
      case Left(msg: RejectBand) => assertEquals(Percent(60), msg.perc)
      case _                     => fail()
    }
  }

  @Test def testRapidTooOkay() {
    val prop = mkProp(Too.rapid)

    // Same instance is returned.
    val block = Block(prop, prop.obsList.head, Time.hours(1), isStart = true, isFinal = false)
    br.reserve(block, queue) match {
      case Right(res) => assertSame(br, res)
      case _          => fail()
    }
  }

  private def verifyRapidTooFail(band: QueueBand, hrs: Int) {
    // Get into the band
    val queue2 = queue :+ mkProp(Time.hours(hrs + 0.1))
    assertEquals(band, queue2.band)

    // Rapid TOO program
    val prop  = mkProp(Too.rapid)
    val block = Block(prop, prop.obsList.head, Time.hours(1), isStart = true, isFinal = false)

    br.reserve(block, queue2) match {
      case Left(msg: RejectBand) => assertEquals(Percent(hrs), msg.perc)
      case _                     => fail()
    }
  }

  @Test def testRapidTooFail() {
    verifyRapidTooFail(QueueBand.QBand2, 30)
    verifyRapidTooFail(QueueBand.QBand3, 60)
  }

  @Test def testStandardTooBand3() {
    // Get into band 3
    val queue2 = queue :+ mkProp(Time.hours(60.1))
    assertEquals(QueueBand.QBand3, queue2.band)

    // Rapid TOO program
    val prop =
      mkProp(Too.standard).copy(band3Observations = obsList(ImageQuality.IQAny, lgs = false))
    val block = Block(prop, prop.obsList.head, Time.hours(1), isStart = true, isFinal = false)

    br.reserve(block, queue2) match {
      case Right(res) => assertSame(br, res)
      case Left(msg)  => fail(msg.reason)
    }
  }

  private def verifyLgsOkay(band: QueueBand, hrs: Int) {
    val queue2 = if (hrs > 0) queue :+ mkProp(Time.hours(hrs + 0.1)) else queue
    assertEquals(band, queue2.band)

    // LGS
    val prop  = mkProp(lgs = true)
    val block = Block(prop, prop.obsList.head, Time.hours(1), isStart = true, isFinal = false)
    br.reserve(block, queue) match {
      case Right(res) => assertSame(br, res)
      case _          => fail()
    }
  }

  @Test def testLgsOkay() {
    verifyLgsOkay(QueueBand.QBand1, 0)
    verifyLgsOkay(QueueBand.QBand2, 30)
  }

  @Test def testLgsFail() {
    val queue2 = queue :+ mkProp(Time.hours(60.1))
    assertEquals(QueueBand.QBand3, queue2.band)

    val prop  = mkProp(lgs = true)
    val block = Block(prop, prop.obsList.head, Time.hours(1), isStart = true, isFinal = false)

    br.reserve(block, queue2) match {
      case Left(msg: RejectBand) => assertEquals(Percent(60), msg.perc)
      case _                     => fail()
    }
  }

  @Test def testNotLgsBand3() {
    val queue2 = queue :+ mkProp(Time.hours(60.1))
    assertEquals(QueueBand.QBand3, queue2.band)

    val prop = mkProp(lgs = false).copy(band3Observations = obsList(ImageQuality.IQAny, lgs = false)
    ) // NOT LGS -- okay in band 3
    val block = Block(prop, prop.obsList.head, Time.hours(1), isStart = true, isFinal = false)

    br.reserve(block, queue2) match {
      case Right(res) => assertSame(br, res)
      case _          => fail()
    }
  }

  private def verifyIQ20Okay(band: QueueBand, hrs: Int) {
    val queue2 = if (hrs > 0) queue :+ mkProp(Time.hours(hrs + 0.1)) else queue
    assertEquals(band, queue2.band)

    // IQ20
    val prop  = mkProp(IQ20, Some(IQ20))
    val block = Block(prop, prop.obsList.head, Time.hours(1), isStart = true, isFinal = false)
    br.reserve(block, queue) match {
      case Right(res) => assertSame(br, res)
      case Left(msg)  => fail(msg.reason)
    }
  }

  @Test def testIQ20Okay() {
    verifyIQ20Okay(QueueBand.QBand1, 0)
    verifyIQ20Okay(QueueBand.QBand2, 30)
  }

  @Test def testIQ20Fail() {
    val queue2 = queue :+ mkProp(Time.hours(60.1))
    assertEquals(QueueBand.QBand3, queue2.band)

    val prop  = mkProp(IQ20, Some(IQ20))
    val block = Block(prop, prop.obsList.head, Time.hours(1), isStart = true, isFinal = false)

    br.reserve(block, queue2) match {
      case Left(msg: RejectBand) => assertEquals(Percent(60), msg.perc)
      case _                     => fail()
    }
  }

  @Test def testIQ70Band3() {
    val queue2 = queue :+ mkProp(Time.hours(60.1))
    assertEquals(QueueBand.QBand3, queue2.band)

    val prop =
      mkProp(IQ70, Some(IQ70)).copy(band3Observations = obsList(ImageQuality.IQAny, lgs = false))
    val block = Block(prop, prop.obsList.head, Time.hours(1), isStart = true, isFinal = false)

    br.reserve(block, queue2) match {
      case Right(res) => assertSame(br, res)
      case _          => fail()
    }
  }

  @Test def testNotB3Fail() {
    val queue2 = queue :+ mkProp(Time.hours(60.1))
    assertEquals(QueueBand.QBand3, queue2.band)

    val prop  = mkProp(lgs = false) // not band 3
    val block = Block(prop, prop.obsList.head, Time.hours(1), isStart = true, isFinal = false)

    br.reserve(block, queue2) match {
      case Left(msg: RejectBand) => assertEquals(Percent(60), msg.perc)
      case _                     => fail()
    }
  }

  private def copyNtac(ref: String, hrs: Int): Ntac =
    ntac.copy(reference = ref, awardedTime = Time.hours(hrs))
  private def obsList(iq: ImageQuality, lgs: Boolean, cc: CloudCover = CloudCover.CCAny) =
    List(Observation(target, conds(iq, cc), Time.hours(10), lgs))

  @Test def testFilterNothing() {
    // Rapid TOO Band 1
    val prop1 = CoreProposal(copyNtac("us1", 30), site = Site.GS, too = Too.rapid)
    // LGS Band 2
    val prop2 =
      CoreProposal(copyNtac("us2", 15), site = Site.GS, obsList = obsList(IQ70, lgs = true))
    // IQ20 Band 2
    val prop3 =
      CoreProposal(copyNtac("us3", 15), site = Site.GS, obsList = obsList(IQ20, lgs = false))
    // Nothing, Band 3
    val prop4 = CoreProposal(
      copyNtac("us4", 40),
      site = Site.GS,
      band3Observations = obsList(ImageQuality.IQ70, lgs = false, CCAny)
    )

    val q2 = queue ++ List(prop1, prop2, prop3, prop4)

    val (q3, log) = br.filter(q2, ProposalLog())

    assertSame(q2, q3)
    assertEquals(Nil, log.toList)
  }

  @Test def testFilter1() {
    // Rapid TOO Band 1
    val prop1 = CoreProposal(copyNtac("us1", 30), site = Site.GS, too = Too.rapid)
    // Rapid TOO Band 2 -- will be removed
    val propX = CoreProposal(copyNtac("usX", 1), site = Site.GS, too = Too.rapid)
    // LGS Band 2
    val prop2 =
      CoreProposal(copyNtac("us2", 15), site = Site.GS, obsList = obsList(IQ70, lgs = true))
    // IQ20 Band 2
    val prop3 =
      CoreProposal(copyNtac("us3", 15), site = Site.GS, obsList = obsList(IQ20, lgs = false))
    // Nothing, Band 3
    val prop4 = CoreProposal(
      copyNtac("us4", 39),
      site = Site.GS,
      band3Observations = obsList(IQ70, lgs = false)
    )

    val q2 = queue ++ List(prop1, propX, prop2, prop3, prop4)

    val (q3, log) = br.filter(q2, ProposalLog())

    assertEquals(List(prop1, prop2, prop3, prop4), q3.toList)
    log.get(Proposal.Id(US, "usX"), QueueBand.Category.B1_2) match {
      case Some(br: RejectBand) => {
        assertEquals(30, br.perc.doubleValue, Double.MinPositiveValue)
        assertEquals(propX, br.prop)
      }
      case _ => fail()
    }
  }

  @Test def testFilterBand3ToBand2() {
    // Rapid TOO Band 1
    val prop1 = CoreProposal(copyNtac("us1", 30), site = Site.GS, too = Too.rapid)
    // Rapid TOO Band 2 -- will be removed
    val propX = CoreProposal(copyNtac("usX", 15), site = Site.GS, too = Too.rapid)
    // LGS Band 2
    val prop2 =
      CoreProposal(copyNtac("us2", 15), site = Site.GS, obsList = obsList(IQ70, lgs = true))

    // IQ20 Band 3 -- but will be moved up 15 hours and back into band 2 when
    // propX is removed.
    val prop3 = CoreProposal(
      copyNtac("us3", 15),
      site = Site.GS,
      band3Observations = obsList(IQ20, lgs = false),
      obsList = obsList(IQ20, lgs = false)
    )

    // Nothing, Band 3
    val prop4 = CoreProposal(
      copyNtac("us4", 25),
      site = Site.GS,
      band3Observations = obsList(IQ70, lgs = false)
    )

    val q2 = queue ++ List(prop1, propX, prop2, prop3, prop4)

    assertEquals(QueueBand.QBand3, q2.positionOf(prop3).get.band) // not legal here

    val (q3, log) = br.filter(q2, ProposalLog())

    assertEquals(QueueBand.QBand2, q3.positionOf(prop3).get.band) // Now it is legal

    assertEquals(List(prop1, prop2, prop3, prop4), q3.toList)
    log.get(Proposal.Id(US, "usX"), QueueBand.Category.B1_2) match {
      case Some(br: RejectBand) => {
        assertEquals(30, br.perc.doubleValue, Double.MinPositiveValue)
        assertEquals(propX, br.prop)
      }
      case _ => fail()
    }
  }

  @Test def testFilterPushIntoBand3() {
    // Rapid TOO Band 1 Joint Part
    val prop1 = CoreProposal(copyNtac("us1", 30), site = Site.GS, too = Too.rapid)
    val jpp1  = JointProposalPart("jpp", prop1)

    // Fill almost all of band 2
    val prop2 = CoreProposal(
      copyNtac("us2", 29),
      site = Site.GS,
      band3Observations = obsList(IQAny, lgs = false)
    )

    // Now, the last band 2 proposal, will be one that doesn't accept band 3.
    val propX = CoreProposal(copyNtac("usX", 2), site = Site.GS)

    // The remainder of the joint which also includes prop1
    val prop3 = CoreProposal(
      copyNtac("us3", 15),
      site = Site.GS,
      band3Observations = obsList(IQAny, lgs = false)
    )
    val jpp3 = JointProposalPart("jpp", prop3)

    val q2 = queue ++ List(jpp1, prop2, propX, jpp3)

    assertEquals(QueueBand.QBand3, q2.positionOf(propX).get.band) // got pushed into band 3

    val expected = q2.toList.filterNot(_.ntac.reference == "usX")

    val (q3, _) = br.filter(q2, ProposalLog())

    assertEquals(expected, q3.toList)
  }

}
