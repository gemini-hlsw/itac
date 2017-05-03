package edu.gemini.tac.qengine.api.queue.time

import edu.gemini.tac.qengine.util.Time
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.p1.QueueBand._

import edu.gemini.tac.qengine.ctx.{TestPartners, Partner, Site}

import org.scalatest._
import org.scalatest.prop._
import Matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExplicitQueueTimeTest extends PropSpec with PropertyChecks with Arbitraries {
  def sum(it: Iterable[Time]): Long =
    it.map(_.ms).sum

  property("Sum of fullPartnerTime is equal to full") {
    forAll { (qt: QueueTime) =>
      sum(qt.fullPartnerTime.map.values) shouldBe qt.full.ms
    }
  }

  property("band1End is equal to band 1 time") {
    forAll { (qt: QueueTime) =>
      qt.band1End shouldBe qt(QBand1)
    }
  }

  property("band2End - band1End is equal to band 2 time") {
    forAll { (qt: QueueTime) =>
      (qt.band2End - qt.band1End) shouldBe qt(QBand2)
    }
  }

  property("band3End - band2End is equal to band 3 time") {
    forAll { (qt: QueueTime) =>
      (qt.band3End - qt.band2End) shouldBe qt(QBand3)
    }
  }
}