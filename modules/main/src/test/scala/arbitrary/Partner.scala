// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package test.arbitrary
import org.scalacheck._
import org.scalacheck.Arbitrary.arbitrary
import edu.gemini.tac.qengine.util.Percent
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.spModel.core.Site

trait PartnerArbitrary {
  import percent._

  val GenPartner: Gen[Partner] =
    for {
      id <- arbitrary[String]
      fn <- arbitrary[String]
      sh <- arbitrary[Percent]
      ss <- arbitrary[Set[Site]]
      e  <- arbitrary[String]
    } yield Partner(id, fn, sh, ss, e)

  implicit val ArbitraryPartner: Arbitrary[Partner] =
    Arbitrary(GenPartner)

}

object partner extends PartnerArbitrary