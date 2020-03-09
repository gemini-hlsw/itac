package edu.gemini.tac.qengine.p2.rollover

import org.junit._
import Assert._

class RolloverReportParserTest {
  import RolloverFixture._

  @Test def testEmptyStringMakesEmptyReport() {
    parser.parse("") match {
      case Right(report) => assertEquals (Nil, report.obsList)
      case _ => fail()
    }
  }

  private def testOne(line: String, expected: RolloverObservation) {
    parser.parse(line) match {
      case Right(RolloverReport(List(obs))) => assertEquals(expected, obs)
      case _ => fail()
    }
  }

  @Test def testValidLineParses() {
    testOne(lineNormal, obsNormal)
  }

  @Test def testSpacesInValuesOkay() {
    testOne(lineSpaces, obsSpaces)
  }

  @Test def testAnyCondsParsed() {
    testOne(lineAnyConds, obsAnyConds)
  }

  private def testThree(lines: List[String]) {
    parser.parse(lines.mkString("\n")) match {
      case Right(RolloverReport(List(obs1, obs2, obs3))) =>
        assertEquals(obsNormal,   obs1)
        assertEquals(obsSpaces,   obs2)
        assertEquals(obsAnyConds, obs3)
      case _ => fail()
    }
  }


  @Test def testMultipleLinesAreHandled() {
    val lines = List(lineNormal, lineSpaces, lineAnyConds)
    testThree(lines)
  }

  @Test def testCommentsAndBlanksAreStripped() {
    val lines = List("#SOF", "", lineNormal, " # A comment ", lineSpaces, "", "", lineAnyConds, "# EOF")
    testThree(lines)
  }

  @Test def testAnInvalidLineIsRejected() {
    parser.parse("xxx") match {
      case Left(RolloverParseError(msg)) =>
        assertEquals(RolloverReportParser.LINE_FORMAT(1, "xxx"), msg)
      case _ =>
        fail()
    }
  }

  @Test def testSingleBadLineRuinsThemAll() {
    val lines = List(lineNormal, lineSpaces, "xxx", lineAnyConds)
    parser.parse(lines.mkString("\n")) match {
      case Left(RolloverParseError(msg)) =>
        assertEquals(RolloverReportParser.LINE_FORMAT(3, "xxx"), msg)
      case _ =>
        fail()
    }
  }

  private def testRejection(line: String, fieldName: String, fieldValue: String) {
    parser.parse(line) match {
      case Left(RolloverParseError(msg)) =>
        assertEquals(RolloverReportParser.BAD_FIELD(1,fieldName,fieldValue), msg)
      case _ =>
        fail()
    }
  }

  @Test def testBadPartnerIsRejected() {
    val s = "XX,GS-2011A-Q-1-10,10:10:10,20:20:20,CC50,IQ20,SB20,WV20,1.0"
    testRejection(s, "partner", "XX")
  }

  @Test def testBadObsIdIsRejected() {
    val s = "AR,GX-2011A-Q-1-10,10:10:10,20:20:20,CC50,IQ20,SB20,WV20,1.0"
    testRejection(s, "obs-id", "GX-2011A-Q-1-10")
  }

  @Test def testBadRaIsRejected() {
    val s = "AR,GS-2011A-Q-1-10,xxx,20:20:20,CC50,IQ20,SB20,WV20,1.0"
    testRejection(s, "RA", "xxx")
  }

  @Test def testBadDecIsRejected() {
    val s = "AR,GS-2011A-Q-1-10,10:10:10,xxx,CC50,IQ20,SB20,WV20,1.0"
    testRejection(s, "dec", "xxx")
  }

  @Test def testBadCcIsRejected() {
    val s = "AR,GS-2011A-Q-1-10,10:10:10,20:20:20,xxx,IQ20,SB20,WV20,1.0"
    testRejection(s, "CC", "xxx")
  }

  @Test def testBadIqIsRejected() {
    val s = "AR,GS-2011A-Q-1-10,10:10:10,20:20:20,CC50,xxx,SB20,WV20,1.0"
    testRejection(s, "IQ", "xxx")
  }

  @Test def testBadSbIsRejected() {
    val s = "AR,GS-2011A-Q-1-10,10:10:10,20:20:20,CC50,IQ20,xxx,WV20,1.0"
    testRejection(s, "SB", "xxx")
  }

  @Test def testBadWvIsRejected() {
    val s = "AR,GS-2011A-Q-1-10,10:10:10,20:20:20,CC50,IQ20,SB20,xxx,1.0"
    testRejection(s, "WV", "xxx")
  }

  @Test def testBadTimeIsRejected() {
    val s = "AR,GS-2011A-Q-1-10,10:10:10,20:20:20,CC50,IQ20,SB20,WV20,xxx"
    testRejection(s, "hours", "xxx")
  }

  @Test def testNegativeTimeIsRejected() {
    val s = "AR,GS-2011A-Q-1-10,10:10:10,20:20:20,CC50,IQ20,SB20,WV20,-1.0"
    testRejection(s, "hours", "-1.0")
  }
}
