// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.api.config

import org.junit._
import Assert._
import edu.gemini.tac.qengine.p1.WaterVapor.WV50
import edu.gemini.tac.qengine.util.{Time, Percent}
import scala.Ordering.Implicits._

class RestrictionConfigTest {
  @Test def testMapCombine() {
    val percentBin = TimeRestriction("WV", Percent(10)) {
      (prop, obs, _) => obs.conditions.wv <= WV50
    }
    val timeBin = TimeRestriction("LGS", Time.hours(10)) {
      (prop, obs, _) => obs.lgs
    }

    val conf = new RestrictionConfig(List(percentBin), List(timeBin))
    val comb = conf.mapTimeRestrictions(_.value.toLong, _.ms)

    assertEquals(2, comb.length)
    assertEquals(10l, comb.head.value)
    assertEquals(10 * 60 * 60 * 1000l, comb.tail.head.value)
  }
}