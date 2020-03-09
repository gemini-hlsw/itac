package edu.gemini.tac.persistence

import org.junit.Test
import org.junit.Assert._
import phase1.Instrument

class InstrumentTest {

  @Test def checkDssi = {
    assertEquals(Instrument.DSSI_GN, Instrument.valueOf("DSSI_GN"))
  }
}
