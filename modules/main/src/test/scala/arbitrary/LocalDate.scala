// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package test.arbitrary
import org.scalacheck._
import java.time.LocalDate

trait LocalDateArbitrary {

  val GenLocalDate: Gen[LocalDate] =
    Arbitrary.arbitrary[Short].map(n => LocalDate.ofEpochDay(n.toLong))

  implicit val ArbitraryLocalDate: Arbitrary[LocalDate] =
    Arbitrary(GenLocalDate)

}

object localdate extends LocalDateArbitrary