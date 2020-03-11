package edu.gemini.tac.qengine.api.config

import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.Site
import org.junit.Test
import org.junit.Assert._


class CustomPartnerSequenceTest {
  @Test
  def doesAdhereToSequenceProportions(): Unit = {
    val site = Site.GN
    val ps = List(
      Partner("A", "A", 42.0, Set(site)),
      Partner("B", "B", 32.0, Set(site)),
      Partner("C", "C", 26.0, Set(site))
    )
    val cps = new CustomPartnerSequence(ps, site)
    val results = cps.sequence.take(990).toList
    assertEquals(List("A", "B", "C", "A", "B", "C"), results.take(6).map(_.id))
    val proportions = results.groupBy(_.id).mapValues(_.size)
    assertEquals(330, proportions("A"))
    assertEquals(330, proportions("B"))
    assertEquals(330, proportions("C"))
  }

  @Test
  def switchToProportional(): Unit = {
    val site = Site.GN
    val ps = List(
      Partner("A", "A", 80.0, Set(site)),
      Partner("B", "B", 10.0, Set(site)),
      Partner("C", "C", 10.0, Set(site))
    )
    val cps = new CustomPartnerSequence(ps, site, "foo", Some(new ProportionalPartnerSequence(ps, site)))
    val results = cps.sequence.take(10000).toList
    assertEquals(List("A", "B", "C", "A", "C", "A"), results.take(6).map(_.id))
    val proportions = results.groupBy(_.id).mapValues(_.size)
    assertEquals(7998, proportions("A"))
    assertEquals(1001, proportions("B"))
    assertEquals(1001, proportions("C"))
  }

  @Test
  def parseCsvSuccess(): Unit = {
    val csv = "A, B,A, C,D,E, F"
    val site = Site.GN
    val ps = List(
      Partner("A", "A", 80.0, Set(site)),
      Partner("B", "B", 10.0, Set(site)),
      Partner("C", "C", 10.0, Set(site)),
      Partner("D", "D", 10.0, Set(site)),
      Partner("E", "E", 10.0, Set(site)),
      Partner("F", "F", 10.0, Set(site))
    )
    val parsed = CsvToPartnerSequenceParser.parse(csv, ps).right.get
    assertEquals(List("A", "B", "A", "C", "D", "E", "F"), parsed.map(_.id))
  }

  @Test
  def parseCsvFailure(): Unit = {
    val csv = "A, B,G, C,D,G, F/H"
    val site = Site.GN
    val ps = List(
      Partner("A", "A", 80.0, Set(site)),
      Partner("B", "B", 10.0, Set(site)),
      Partner("C", "C", 10.0, Set(site)),
      Partner("D", "D", 10.0, Set(site)),
      Partner("E", "E", 10.0, Set(site)),
      Partner("F", "F", 10.0, Set(site))
    )
    val errors = CsvToPartnerSequenceParser.parse(csv, ps).left.get
    assertEquals(Set("G", "H"), errors)
  }
}