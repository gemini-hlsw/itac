// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package test.arbitrary
import org.scalacheck._
import edu.gemini.tac.qengine.ctx.Partner

trait PartnerArbitrary {

  val GenPartner: Gen[Partner] =
    Gen.oneOf(Partner.all)

  implicit val ArbitraryPartner: Arbitrary[Partner] =
    Arbitrary(GenPartner)

}

object partner extends PartnerArbitrary