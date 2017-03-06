package edu.gemini.tac.persistence.phase1

import org.junit._
import Assert._
import edu.gemini.shared.skycalc.Angle
import java.text.SimpleDateFormat
import java.util.{ArrayList, Date}

class NonsiderealTargetTest {

  def coordinates(ra: Double, dec: Double): Coordinates = {
    val c = new DegDegCoordinates(new Angle(ra, Angle.Unit.DEGREES), new Angle(dec, Angle.Unit.DEGREES))
    c
  }

  def date(s: String): Date = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").parse(s)

  @Test def testFindSingleEphemeris() {
    val nst = new NonsiderealTarget();
    val d = date("2012.02.01.00.00.00")
    val ee1 = new EphemerisElement()
    ee1.setCoordinates(coordinates(0, 0))
    ee1.setValidAt(d)

    val ees = new ArrayList[EphemerisElement]()
    ees.add(ee1)
    nst.setEphemeris(ees)

    val maybeC = nst.findOrNull(d)
    assertSame(ee1.getCoordinates, maybeC)
  }

  @Test def testFindSimple() {
    val nst = new NonsiderealTarget();
    val d = date("2012.02.01.00.00.00")
    val ee1 = new EphemerisElement()
    ee1.setCoordinates(coordinates(0, 0))
    ee1.setValidAt(d)
    val ee2 = new EphemerisElement()
    ee2.setCoordinates(coordinates(180, 90))
    ee2.setValidAt(date("2012.02.03.00.00.00"))

    val ees = new ArrayList[EphemerisElement]()
    ees.add(ee1)
    ees.add(ee2)
    nst.setEphemeris(ees)

    val maybeC = nst.findOrNull(date("2012.02.02.00.00.00"))
    assertTrue(90.0 == maybeC.getRa.getMagnitude)
    assertTrue(45.0 == maybeC.getDec.getMagnitude)
  }

  @Test def testFindInList() {
    val nst = new NonsiderealTarget();
    val d = date("2012.02.01.00.00.00")
    val ee1 = new EphemerisElement()
    ee1.setCoordinates(coordinates(0, 0))
    ee1.setValidAt(d)
    val ee2 = new EphemerisElement()
    ee2.setCoordinates(coordinates(50, 50))
    ee2.setValidAt(date("2012.02.03.00.00.00"))
    val ee3 = new EphemerisElement()
    ee3.setCoordinates(coordinates(60, 60))
    ee3.setValidAt(date("2012.02.05.00.00.00"))

    val ees = new ArrayList[EphemerisElement]()
    ees.add(ee1)
    ees.add(ee2)
    ees.add(ee3)
    nst.setEphemeris(ees)

    val maybeC = nst.findOrNull(date("2012.02.04.00.00.00"))
    assertTrue(55.0 == maybeC.getRa.getMagnitude)
    assertTrue(55.0 == maybeC.getDec.getMagnitude)
  }
}