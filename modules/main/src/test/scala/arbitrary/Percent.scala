// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package test.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Arbitrary
import edu.gemini.tac.qengine.util.Percent

trait PercentArbitrary {

  val GenPercent: Gen[Percent] =
    Arbitrary.arbitrary[BigDecimal].map(Percent(_))

  implicit val ArbitraryPercent: Arbitrary[Percent] =
    Arbitrary(GenPercent)

}

object percent extends PercentArbitrary