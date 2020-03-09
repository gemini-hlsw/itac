package edu.gemini.tac.qengine.impl

import block.Block
import org.junit._
import Assert._
import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1.{ObservingConditions, Target, Ntac}
import edu.gemini.tac.qengine.api.config.SiteSemesterConfig
import resource.{RaResourceGroup, Fixture}
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.core.Semester

class TooBlocksTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  val site     = Site.GS
  val semester = new Semester(2011, Semester.Half.A)

  val target = Target(0.0, 0.0) // not used for ToO observations

  // Tests the unusual case in which there is sufficient time remaining in all
  // RA bins to handle an even distribution of a ToO observation.
  @Test def testEvenDistribution() {
    // Map to 1 .. 24 hours instead of the Fixture's 0 .. 23
    val raLimits = Fixture.raLimits.map(_ + Time.hours(1))
    val binConfig = new SiteSemesterConfig(
      site,
      semester,
      raLimits,
      Fixture.decBins,
      List.empty,
      Fixture.condsBins
    )
    val raResGroup = RaResourceGroup(binConfig)

    val hrs12 = Time.hours(12.0)
    val ntac  = Ntac(US, "US1", 1, hrs12)
    val prop  = Fixture.mkProp(ntac, (target, ObservingConditions.AnyConditions, hrs12))

    // One big block with a 12 hour ToO observation.  (I know, that would never
    // happen in real life.)
    val block  = Block(prop, prop.obsList.head, hrs12)
    val blocks = raResGroup.tooBlocks(block).get

    // Expecting to see 24 half hour blocks.
    assertEquals(24, blocks.size)

    assertEquals(Time.minutes(30.0), blocks.head.time)
    assertEquals(None, blocks.find(_.time != Time.minutes(30.0)))
  }

  // Tests the normal case in which at least one block is already full.
  @Test def testFullBlock() {
    val hrs23 = Time.hours(23.0)
    val ntac  = Ntac(US, "US1", 1, hrs23)
    val prop  = Fixture.mkProp(ntac, (target, ObservingConditions.AnyConditions, hrs23))

    // One big block with a 23 hour ToO observation.  (I know, that would never
    // happen in real life.)
    val block  = Block(prop, prop.obsList.head, hrs23)
    val blocks = Fixture.raResGroup.tooBlocks(block).get

    // We should be able to distribute over RAs 1 .. 23.  RA 0 has zero time
    // though.  So each of the 23 bins should have an equal share of the
    // 23 hours (that is, 1 hour per block)
    assertEquals(24, blocks.size)

    assertEquals(Time.Zero, blocks.head.time)
    assertEquals(Time.hours(1), blocks.tail.head.time)
    assertEquals(None, blocks.tail.find(_.time != Time.hours(1.0)))
  }

  // Tests the normal case in which one or more bins are not big enough to
  // hold all the time in an even distribution.  The remaining time has to
  // be spread over the other blocks
  @Test def testSmallBlocks() {
    val hrs  = Time.hours(66.0)
    val ntac = Ntac(US, "US1", 1, hrs)
    val prop = Fixture.mkProp(ntac, (target, ObservingConditions.AnyConditions, hrs))

    // One big block with a 69 hour ToO observation.  (I know, that would never
    // happen in real life.)
    val block  = Block(prop, prop.obsList.head, hrs)
    val blocks = Fixture.raResGroup.tooBlocks(block).get

    // We should be able to distribute over RAs 1 .. 23.
    // RA 0 -> 0 hr block
    //    1 -> 1 hr block
    //    2 -> 2 hr block
    //    3 -> 3 hr block
    //    4 -> 3 hr block
    //    5 -> 3 hr block
    //   23 -> 3 hr block
    assertEquals(24, blocks.size)

    assertEquals(Time.Zero, blocks.head.time)
    assertEquals(Time.hours(1), blocks.tail.head.time)
    assertEquals(Time.hours(2), blocks.tail.tail.head.time)
    assertEquals(Time.hours(3), blocks.tail.tail.tail.head.time)
    assertEquals(None, blocks.tail.tail.tail.find(_.time != Time.hours(3.0)))
  }

  // An extreme case in which every bin is maxed out in order to accommodate the
  // ToO.
  @Test def testMax() {
    val hrs  = Time.hours(276.0) // (0 + 1 + 2 + ... 23) = (23*24/2)
    val ntac = Ntac(US, "US1", 1, hrs)
    val prop = Fixture.mkProp(ntac, (target, ObservingConditions.AnyConditions, hrs))

    // One big block with a 276 hour ToO observation.  (I know, that would never
    // happen in real life.)
    val block  = Block(prop, prop.obsList.head, hrs)
    val blocks = Fixture.raResGroup.tooBlocks(block).get

    blocks.zipWithIndex.foreach(tup => {
      val b = tup._1
      val i = tup._2
      assertEquals(Time.hours(i), b.time)
    })
  }

  // Tests the unusual case in which we simply cannot split the observation up
  // because there isn't time remaining in the RA bins.
  @Test def testNone() {
    val hrs  = Time.hours(277.0) // (0 + 1 + 2 + ... 23) + 1 = (23*24/2) + 1
    val ntac = Ntac(US, "US1", 1, hrs)
    val prop = Fixture.mkProp(ntac, (target, ObservingConditions.AnyConditions, hrs))

    // One big block with a 277 hour ToO observation.  (I know, that would never
    // happen in real life.)
    val block = Block(prop, prop.obsList.head, hrs)
    assertEquals(None, Fixture.raResGroup.tooBlocks(block))
  }
}
