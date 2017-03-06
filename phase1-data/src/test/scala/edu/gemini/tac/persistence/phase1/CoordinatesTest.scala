package edu.gemini.tac.persistence.phase1

import org.junit._
import Assert._

class CoordinatesTest {
  @Test def testHmsDmsCoordinatesParse(){
    val c = new HmsDmsCoordinates()
    c.setRa("10:00:00")
    c.setDec("80:30:00")
    assertEquals(150.0, c.getRa.getMagnitude, 0.00001)
    assertEquals(80.5, c.getDec.getMagnitude, 0.0001)
  }

  @Test def testHmsDmsThrowsOnInvalid(){
    val c = new HmsDmsCoordinates()
    var threw = false
    try{
      c.setRa("XX")
    }catch {
      case _:Exception => threw = true

    }
    assertTrue(threw)
    threw = false
    try{
      c.setDec("XX")
    }catch{
      case _:Exception => threw = true
    }
    assertTrue(threw)
  }

  @Test def testHmsDmsTrimsRaStrings(){
    val c = new HmsDmsCoordinates()
    c.setRa(" 00:05:52.50 ")
    //Just assure that we get here
    assertNotNull(c)
  }

  @Test def testHmsDmsTrimsDecStrings(){
    val c = new HmsDmsCoordinates()
    c.setDec(" 80:30:00 ")
    //Assure we get here
    assertNotNull(c)
  }

}