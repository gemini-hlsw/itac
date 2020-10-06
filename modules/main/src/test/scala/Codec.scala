// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package test

import cats.tests.CatsSuite
import edu.gemini.tac.qengine.util.Percent
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import itac.codec.all._
import java.time.LocalDate
import org.scalacheck.Arbitrary
import test.arbitrary.all._
import org.scalatest.matchers.should.Matchers
import edu.gemini.spModel.core.Site

class CodecSuite extends CatsSuite with Matchers {

  // Ensure that encode andThen decode = id (this is a split monomorphism)
  def codec[A: Arbitrary: Encoder: Decoder](name: String) =
    test(name) {
      forAll { (a: A) =>
        decode[A](a.asJson.spaces2) shouldBe Right(a)
      }
    }

  codec[Site]("site")
  codec[Percent]("percent")
  codec[(LocalDate, LocalDate)]("localdaterange")

  // TODO: many more

}

