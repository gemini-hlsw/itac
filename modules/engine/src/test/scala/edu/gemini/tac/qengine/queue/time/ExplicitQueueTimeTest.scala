package edu.gemini.tac.qengine.api.queue.time

import edu.gemini.tac.qengine.util.{Percent, Time}
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.p1.QueueBand._

import edu.gemini.tac.qengine.ctx.{TestPartners,Partner}
import edu.gemini.spModel.core.Site

import org.scalatest._
import org.scalatest.prop._
import Matchers._
import org.junit.runner.RunWith

class ExplicitQueueTimeTest extends PropSpec with org.scalatestplus.scalacheck.Checkers with Arbitraries {
  def sum(it: Iterable[Time]): Long =
    it.map(_.ms).sum

  property("Sum of fullPartnerTime is equal to full") {
    check { (qt: QueueTime) =>
      sum(qt.fullPartnerTime.map.values) == qt.full.ms
    }
  }

  property("band1End is equal to band 1 time") {
    check { (qt: QueueTime) =>
      qt.band1End == qt(QBand1)
    }
  }

  property("band2End - band1End is equal to band 2 time") {
    check { (qt: QueueTime) =>
      (qt.band2End - qt.band1End) == qt(QBand2)
    }
  }

  property("band3End - band2End is equal to band 3 time") {
    check { (qt: QueueTime) =>
      (qt.band3End - qt.band2End) == qt(QBand3)
    }
  }

  property("sum of partner time in each queue band = full partner time") {
    check { (qt: QueueTime) =>
      TestPartners.All.forall { p =>
        sum(QueueBand.values.map(qt(_, p))) == qt(p).ms
      }
    }
  }

  property("sum of band time for all partners = band time") {
    check { (qt: QueueTime) =>
      QueueBand.values.forall { b =>
        sum(TestPartners.All.map(qt(b, _))) == qt(b).ms
      }
    }
  }

  property("queband% for each band should be the ratio of corresponding band time to full time") {
    check { (qt: QueueTime) =>
      val bp    = qt.bandPercentages
      val percs = QueueBand.values.zip(List(bp.band1, bp.band2, bp.band3))
      val full  = qt.band3End.ms.toDouble

      (full == 0.0) || percs.forall { case (band, perc) =>
        val ratio = Percent.fromQuotient(qt(band).ms / full)
        (perc - ratio).value.abs <= BigDecimal(1, 2)
      }
    }
  }
}