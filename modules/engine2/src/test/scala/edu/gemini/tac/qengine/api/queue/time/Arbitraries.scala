package edu.gemini.tac.qengine.api.queue.time

import org.scalacheck._
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary._

import edu.gemini.tac.qengine.ctx.{Partner, TestPartners}
import edu.gemini.tac.qengine.p1.QueueBand
import edu.gemini.tac.qengine.util.{Percent, Time}

trait Arbitraries {
  implicit val arbitraryPartner: Arbitrary[Partner] =
    Arbitrary {
      oneOf(TestPartners.All)
    }

  implicit val arbitraryQueueBand: Arbitrary[QueueBand] =
    Arbitrary {
      oneOf(QueueBand.values)
    }

  implicit val arbitraryTime: Arbitrary[Time] =
    Arbitrary {
      posNum[Long].map { Time.millisecs }
    }

  implicit val arbitraryPercent: Arbitrary[Percent] =
    Arbitrary {
      choose(0.0, 1.0).map { Percent.fromQuotient }
    }

  implicit val arbitraryQueueTime: Arbitrary[QueueTime] =
    Arbitrary {
      for {
        m <- arbitrary[Map[(Partner, QueueBand), Time]]
        p <- arbitrary[Option[Percent]]
      } yield new ExplicitQueueTime(m, p)
    }
}
