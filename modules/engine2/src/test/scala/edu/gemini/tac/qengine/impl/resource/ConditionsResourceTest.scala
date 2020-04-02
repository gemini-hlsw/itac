package edu.gemini.tac.qengine.impl.resource

import edu.gemini.tac.qengine.log.RejectConditions
import org.junit._
import Assert._

import edu.gemini.tac.qengine.p1._
import CloudCover._
import ImageQuality.IQ20
import SkyBackground.SB20
import WaterVapor.WV20
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.api.config.{ConditionsBinGroup, ConditionsBin, ConditionsCategory => Cat}
import Cat._
import edu.gemini.tac.qengine.util.{Percent, Time}
import edu.gemini.tac.qengine.impl.block.Block
import edu.gemini.tac.qengine.api.queue.time.QueueTime
import edu.gemini.tac.qengine.impl.queue.ProposalQueueBuilder
import edu.gemini.spModel.core.Site

class ConditionsResourceTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  private val bins = ConditionsBin.of(
      (Cat(Eq(CC50)),  Percent(25)),
      (Cat(Eq(CC70)),  Percent(25)),
      (Cat(Eq(CC80)),  Percent(25)),
      (Cat(Eq(CCAny)), Percent(25))
    )

  private val binGrp = ConditionsBinGroup.of(bins)
  private val resGrp = ConditionsResourceGroup(Time.minutes(100), binGrp)

  private val ntac   = Ntac(GS, "x", 0, Time.minutes(100)) // not used
  private val target = Target(0,0)                               // not used

  private def mkProp(obsConds: ObservingConditions): CoreProposal = {
    val obsList = List(Observation(null, target, obsConds, Time.minutes(10)))
    CoreProposal(ntac, site = Site.GS, obsList = obsList)
  }

  private def mkConds(cc: CloudCover): ObservingConditions =
    ObservingConditions(cc, IQ20, SB20, WV20)

  // Verify that the given remaining times match -- times must be specified
  // in order of CloudCover values.
  private def verifyTimes(track: ConditionsResourceGroup, mins: Int*) {
    CloudCover.values.zip(mins).foreach {
      case (cc, min) => {
        val remaining = track.remaining(mkConds(cc))
        assertEquals(Time.minutes(min), remaining)
      }
    }
  }

  private def testSuccess(time: Time, cnds: ObservingConditions, mins: Int*) {
    val (newResGrp, rem) = resGrp.reserveAvailable(time, cnds)
    assertEquals(Time.Zero, rem)
    verifyTimes(newResGrp, mins: _*)

    val prop  = mkProp(cnds)
    val otb   = Block(prop, prop.obsList.head, time)
    resGrp.reserve(otb, Fixture.emptyQueue) match {
      case Right(res) => verifyTimes(res, mins: _*)
      case _ => fail()
    }
  }

  @Test def testSimpleReservationThatRequiresNoTimeFromABetterBin() {
    val time  = Time.minutes(10)
    val cnds  = mkConds(CC70)

    // Given 25 minutes for CC70, we should be able to fully reserve the time
    // in the corresponding bin.
    // CC50:  25 -  0 = 25 (25 <- 25)
    // CC70:  25 - 10 = 15 (40 <- 15 + 25)
    // CC80:  25 -  0 = 25 (65 <- 25 + 15 + 25)
    // Any.:  25 -  0 = 25 (90 <- 25 + 25 + 15 + 25)
    testSuccess(time, cnds, 25, 40, 65, 90)
  }

  @Test def testStealTimeFromABetterBin() {
    val time  = Time.minutes(51)
    val cnds  = mkConds(CC80)

    // Given 51 minutes for CC80, we use all 25 minutes of CC80, all 25 of CC70,
    // and 1 minute of CC50.
    // CC50:  25 -  1 = 24 (24 <- 24)
    // CC70:  25 - 25 =  0 (24 <-  0 + 24)
    // CC80:  25 - 25 =  0 (24 <-  0 +  0 + 24)
    // Any.:  25 -  0 = 25 (49 <- 25 +  0 +  0 + 24)
    testSuccess(time, cnds, 24, 24, 24, 49)
  }

  @Test def testStealExactlyAllRemainingTimeFromBetterBins() {
    val time  = Time.minutes(75)
    val cnds  = mkConds(CC80)

    // CC50:  25 - 25 =  0 ( 0 <-  0)
    // CC70:  25 - 25 =  0 ( 0 <-  0 +  0)
    // CC80:  25 - 25 =  0 ( 0 <-  0 +  0 +  0)
    // Any.:  25 -  0 = 25 (25 <- 25 +  0 +  0 + 0)
    testSuccess(time, cnds, 0, 0, 0, 25)
  }

  @Test def testAttemptToReserveMoreThanAvailable() {
    val (newGrp, rem) = resGrp.reserveAvailable(Time.minutes(76), mkConds(CC80))
    verifyTimes(newGrp, 0, 0, 0, 25)
    assertEquals(Time.minutes(1), rem) // 1 minute could not be reserved
  }

  @Test def testCannotStealMoreThanThanAvailableFromBetterBins() {
    val prop = mkProp(mkConds(CC80))

    val otb2 = Block(prop, prop.obsList.head, Time.minutes(76))
    resGrp.reserve(otb2, Fixture.emptyQueue) match {
      case Left(rc: RejectConditions) => // pass
      case _ => fail()
    }
  }

  @Test def testBand3ConditionsUsedinBand3() {
    // Create a proposal to fill the first two queue bands and put us into
    // band 3.
    val q0 = ProposalQueueBuilder(QueueTime(Site.GS, Map(GS -> Time.minutes(166)), partners))
    val q1 = q0 :+ mkProp(mkConds(CC50))
    assertEquals(QueueBand.QBand3, q1.band)
    val template : Observation = mkProp(mkConds(CC50)).obsList.head
    val templateConditions = template.conditions
    val cs = ObservingConditions(CloudCover.CC80, templateConditions.iq, templateConditions.sb, templateConditions.wv)
    val obs = template.copy(conditions=cs)
    assertEquals(Time.minutes(10), obs.time)
    val b3os = List(obs)

    val prop  = mkProp(mkConds(CC50)).copy(band3Observations = b3os)
    val block = Block(prop, prop.band3Observations.head, Time.minutes(10))

    verifyTimes(resGrp, 25, 50, 75, 100) //Each condition has +25 minutes
    val res   = resGrp.reserve(block, q1)
    verifyTimes(res.right.get, 25, 50, 65, 90) // uses CC80 time, not CC50 (-10 @ 80, but then +25 to 90)
  }
}