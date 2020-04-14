package edu.gemini.tac.qengine.impl

import org.junit._
import Assert._
import resource.{RaResourceGroup, Fixture}

class RaResourceInitTest {
  val baseline = RaResourceGroup(Fixture.binConfig)


  @Test def testInitBinsNoAdjustments() {
    val init = QueueEngine.initBins(Fixture.binConfig, Nil, Nil)

    val bList = baseline.grp.bins
    val iList = init.grp.bins

    bList.zip(iList) foreach { case (a, b) => assertEquals(a.absBounds, b.absBounds) }
  }

}