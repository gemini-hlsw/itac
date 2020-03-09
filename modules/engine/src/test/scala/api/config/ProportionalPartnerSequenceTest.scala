package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.ctx.Partner

import org.junit.Test
import org.junit.Assert._
import edu.gemini.spModel.core.Site

class ProportionalPartnerSequenceTest {
  @Test
  def doesAdhereToPortions(): Unit = {
    val site = Site.GN
    val ps = List(
      Partner("A", "A", 42.0, Set(site)),
      Partner("B", "B", 32.0, Set(site)),
      Partner("C", "C", 26.0, Set(site))
    )
    val pps         = new ProportionalPartnerSequence(ps, site)
    val results     = pps.sequence.take(100).toList
    val proportions = results.groupBy(_.id).mapValues(_.size)
    assertEquals(42, proportions("A"))
    assertEquals(32, proportions("B"))
    assertEquals(26, proportions("C"))
  }

  @Test
  def doesFilterOutOtherSites(): Unit = {
    val site = Site.GN
    val ps = List(
      Partner("A", "A", 42.0, Set(site)),
      Partner("B", "B", 32.0, Set(site)),
      Partner("C", "C", 26.0, Set(site, Site.GS)),
      Partner("D", "D", 25.0, Set(Site.GS))
    )
    val pps         = new ProportionalPartnerSequence(ps, site)
    val results     = pps.sequence.take(100).toList
    val proportions = results.groupBy(_.id).mapValues(_.size)
    assertEquals(42.0, proportions("A"), 0.000001)
    assertEquals(32.0, proportions("B"), 0.000001)
    assertEquals(26.0, proportions("C"), 0.000001)
  }
  @Test
  def doesCycleProperlyEvenIfSomePartnerHasZero(): Unit = {
    val site = Site.GN
    val ps = List(
      Partner("A", "A", 42.0, Set(site)),
      Partner("B", "B", 32.0, Set(site)),
      Partner("C", "C", 26.0, Set(site, Site.GS)),
      Partner("D", "D", 25.0, Set(Site.GS)),
      Partner("E", "E", 0.0, Set(site))
    )
    val pps         = new ProportionalPartnerSequence(ps, site)
    val results     = pps.sequence.take(200).toList
    val proportions = results.groupBy(_.id).mapValues(_.size)
    assertEquals(84, proportions("A"), 0.000001)
    assertEquals(64, proportions("B"), 0.000001)
    assertEquals(52, proportions("C"), 0.000001)
  }

  @Test
  def doesDriveTowardsProportionalityEvenIfFirstPartnerIsOverridden(): Unit = {
    val site = Site.GN
    val c    = Partner("C", "C", 25.0, Set(site))
    val ps = List(
      Partner("A", "A", 50.0, Set(site)),
      Partner("B", "B", 25.0, Set(site)),
      c
    )
    val seqNoOverride     = new ProportionalPartnerSequence(ps, site)
    val resultsNoOverride = seqNoOverride.sequence.take(11).toList
    val seqWithOverride   = new ProportionalPartnerSequence(ps, site, c)
    val resultsOverride   = seqWithOverride.sequence.take(10).toList

    assertEquals("A,C,B,A,A,C,B,A,A,C,B", resultsNoOverride.map(_.id).mkString(","))
    //Initial "A" is dropped, but calcs are as if it were included...
    assertEquals("C,B,A,A,C,B,A,A,C,B", resultsOverride.map(_.id).mkString(","))

    //Drives towards proportions
    val longEnough  = seqWithOverride.sequence.take(100).toList
    val proportions = longEnough.groupBy(_.id).mapValues(_.size)
    assertEquals(50, proportions("A"), 0.000001)
    assertEquals(25, proportions("B"), 0.000001)
    assertEquals(25, proportions("C"), 0.000001)
  }
}
