package edu.gemini.tac.psconversion

import org.junit._
import Assert._

import edu.gemini.tac.qengine.ctx.Site

class SiteParserTest {
  @Test def testNorth() {
    val norths = List(
      "north",
      "North",
      "NORTH",
      "Gemini North",
      "Gemini North Observatory",
      "gn",
      "GN",
      "GN Observatory",
      "*GN*")

    norths.foreach {
      s => assertEquals(Site.north, SiteConverter.parse(s) | Site.south)
    }
  }

  @Test def testSouth() {
    val souths = List(
      "south",
      "South",
      "SOUTH",
      "Gemini South",
      "Gemini South Observatory",
      "gs",
      "GS",
      "GS Observatory",
      "*GS*")

    souths.foreach {
      s => assertEquals(Site.south, SiteConverter.parse(s) | Site.north)
    }
  }

  @Test def testError() {
    val errors = List(
      "",
      "neither"
    )

    errors.foreach { s =>
      assertEquals(BadData(SiteConverter.UNRECOGNIZED_SITE(s)), SiteConverter.parse(s).toEither.left.get)
    }
  }
}