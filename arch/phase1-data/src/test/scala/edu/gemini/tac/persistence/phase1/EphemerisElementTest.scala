package edu.gemini.tac.persistence.phase1

import org.junit._
import Assert._
import java.util.Date
import edu.gemini.shared.skycalc.Angle
import java.text.SimpleDateFormat

class EphemerisElementTest {

  def coordinates(ra: Double, dec: Double): Coordinates = {
    val c = new DegDegCoordinates(new Angle(ra, Angle.Unit.DEGREES), new Angle(dec, Angle.Unit.DEGREES))
    c
  }

  def date(s: String): Date = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").parse(s)

  @Test def testStationaryInterpolation() {
    val d = date("2012.02.01.00.00.00")
    val ee1 = new EphemerisElement()
    ee1.setCoordinates(coordinates(0, 0))
    ee1.setValidAt(d)
    val ee2 = new EphemerisElement()
    ee2.setCoordinates(coordinates(0, 0))
    ee2.setValidAt(date("2012.02.03.00.00.00"))

    val interpolate = ee1.interpolate(date("2012.02.02.00.00.00"), ee2)
    assertTrue(interpolate.getRa.getMagnitude == 0.0)
    assertTrue(interpolate.getDec.getMagnitude == 0.0)
  }

  @Test
  def testInterpolation() {
    val d = date("2012.02.01.00.00.00")
    val ee1 = new EphemerisElement()
    ee1.setCoordinates(coordinates(0, 0))
    ee1.setValidAt(d)
    val ee2 = new EphemerisElement()
    ee2.setCoordinates(coordinates(180, 90))
    ee2.setValidAt(date("2012.02.03.00.00.00"))

    val interpolate = ee1.interpolate(date("2012.02.02.00.00.00"), ee2)
    assertTrue(interpolate.getRa.getMagnitude == 90.0)
    assertTrue(interpolate.getDec.getMagnitude == 45.0)
  }

  @Test def testNegativeInterpolation() {
    val d = date("2012.02.01.00.00.00")
    val ee1 = new EphemerisElement()
    ee1.setCoordinates(coordinates(90, 40))
    ee1.setValidAt(d)
    val ee2 = new EphemerisElement()
    ee2.setCoordinates(coordinates(0, 0))
    ee2.setValidAt(date("2012.02.03.00.00.00"))

    val interpolate = ee1.interpolate(date("2012.02.02.00.00.00"), ee2)
    assertTrue(interpolate.getRa.getMagnitude == 45.0)
    assertTrue(interpolate.getDec.getMagnitude == 20.0)

  }
}