package edu.gemini.tac.qengine.api.config

import org.junit._
import Assert._
import edu.gemini.tac.qengine.ctx.Site

class PartnerSequenceTest {
  import edu.gemini.tac.qengine.ctx.TestPartners._
  val partners = All

  @Test def initialUS() { assertEquals(US, new ProportionalPartnerSequence(partners, Site.north, US).sequence.head) }
  @Test def initialUH() { assertEquals(UH, new ProportionalPartnerSequence(partners, Site.north, UH).sequence.head) }
  @Test def initialBR() { assertEquals(BR, new ProportionalPartnerSequence(partners, Site.north, BR).sequence.head) }

  @Test def basics() { assertEquals(List(US, CA, UH), new ProportionalPartnerSequence(partners, Site.north, US).sequence.take(3).toList) }

  @Test def testSite() {
    Site.values.foreach { site =>
      new ProportionalPartnerSequence(partners, site).sequence.take(100).foreach {
        partner => assertTrue(partner.sites.contains(site))
      }
    }
  }

  @Test def testBadInitial() {
    try {
      new ProportionalPartnerSequence(partners, Site.north, CL)
      fail()
    } catch {
      case ex: IllegalArgumentException => // ok
      case x : Exception => fail("Unexpected " + x)
    }
  }

   @Test def adheresToPartnerPercentages() {
    Site.values.foreach { site =>
      val first100 = new ProportionalPartnerSequence(partners, site).sequence.take(100).toList
      val counts = first100.groupBy(identity).mapValues(_.size)
      val sumOfAllocations = partners.foldRight(0.0)(_.percentAt(site) + _)
      partners.foreach { p =>
        val expected = p.percentAt(site) / sumOfAllocations
        val achieved =
          counts.contains(p) match {
            case true => counts(p) / 100.0
            case false => 0
        }
        assert(Math.abs(expected - achieved) < 1.0)
      }
    }
  }

  @Test
  def xmlRepresentation() {
    val elem = new ProportionalPartnerSequence(partners, Site.north).configuration
    assertTrue(elem.size > 0)
  }
}