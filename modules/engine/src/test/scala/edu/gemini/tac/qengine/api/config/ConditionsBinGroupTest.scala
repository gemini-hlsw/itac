// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.api.config

import org.junit._
import Assert._

import edu.gemini.tac.qengine.api.config.ConditionsCategory.Eq
import edu.gemini.tac.qengine.p1.CloudCover._
import edu.gemini.tac.qengine.p1.ImageQuality._
import edu.gemini.tac.qengine.p1.SkyBackground._
import edu.gemini.tac.qengine.p1.WaterVapor._
import edu.gemini.tac.qengine.util.Percent
import edu.gemini.tac.qengine.p1.ObservingConditions

class ConditionsBinGroupTest {
  val cat0 = ConditionsCategory(Eq(CC50))
  val cat1 = ConditionsCategory(Eq(CC70))
  val cat2 = ConditionsCategory(Eq(CC80))
  val cat3 = ConditionsCategory(Eq(CCAny))
  val bin0 = ConditionsBin(cat0, Percent(10))
  val bin1 = ConditionsBin(cat1, Percent(20))
  val bin2 = ConditionsBin(cat2, Percent(30))
  val bin3 = ConditionsBin(cat3, Percent(40))
  val oc0  = ObservingConditions(CC50,  IQ20, SB20, WV20)
  val oc1  = ObservingConditions(CC70,  IQ20, SB20, WV20)
  val oc2  = ObservingConditions(CC80,  IQ20, SB20, WV20)
  val oc3  = ObservingConditions(CCAny, IQ20, SB20, WV20)

  val grp = ConditionsBinGroup.of(List(bin0, bin1, bin2, bin3))

  def validatePath(grp: ConditionsBinGroup[_]) {
    assertEquals(List(cat0), grp.searchPath(oc0))
    assertEquals(List(cat1, cat0), grp.searchPath(oc1))
    assertEquals(List(cat2, cat1, cat0), grp.searchPath(oc2))
    assertEquals(List(cat3, cat2, cat1, cat0), grp.searchPath(oc3))
  }

  @Test def testCreation() {
    assertEquals(4, grp.bins.size)
    assertEquals(Percent(10), grp.bins(cat0))
    assertEquals(Percent(20), grp.bins(cat1))
    assertEquals(Percent(30), grp.bins(cat2))
    assertEquals(Percent(40), grp.bins(cat3))
    validatePath(grp)
  }

  @Test def testMap() {
    // map percents to integer values
    val grp2 = grp.map(_.value)
    assertEquals(4, grp2.bins.size)
    assertEquals(10, grp2.bins(cat0).toDouble, Double.MinPositiveValue)
    assertEquals(20, grp2.bins(cat1).toDouble, Double.MinPositiveValue)
    assertEquals(30, grp2.bins(cat2).toDouble, Double.MinPositiveValue)
    assertEquals(40, grp2.bins(cat3).toDouble, Double.MinPositiveValue)
    validatePath(grp2)
  }

  @Test def testLookup() {
    assertEquals(Percent(10), grp(cat0))
    assertEquals(Percent(20), grp(cat1))
    assertEquals(Percent(30), grp(cat2))
    assertEquals(Percent(40), grp(cat3))
  }

  @Test def testSimpleUpdated() {
    val grp2 = grp.updated(cat2, Percent(42))
    assertEquals(Percent(10), grp2(cat0))
    assertEquals(Percent(20), grp2(cat1))
    assertEquals(Percent(42), grp2(cat2))
    assertEquals(Percent(40), grp2(cat3))
    validatePath(grp2)
  }

  @Test def testSimpleUpdatedOC() {
    val grp2 = grp.updated(oc2, Percent(42))
    assertEquals(Percent(10), grp2(cat0))
    assertEquals(Percent(20), grp2(cat1))
    assertEquals(Percent(42), grp2(cat2))
    assertEquals(Percent(40), grp2(cat3))
    validatePath(grp2)
  }

  @Test def testMultipleUpdated() {
    val grp2 = grp.updated(List((cat1, Percent(11)), (cat2, Percent(22))))
    assertEquals(Percent(10), grp2(cat0))
    assertEquals(Percent(11), grp2(cat1))
    assertEquals(Percent(22), grp2(cat2))
    assertEquals(Percent(40), grp2(cat3))
    validatePath(grp2)
  }

  @Test def testUndefinedSimpleUpdated() {
    try {
      grp.updated(ConditionsCategory(Eq(CC50), Eq(IQ20)), Percent(99))
      fail
    } catch {
      case _: IllegalArgumentException => // ok
    }
  }

  @Test def testUndefinedMuliptleUpdated() {
    val cat4 = ConditionsCategory(Eq(CC50), Eq(IQ20))
    try {
      grp.updated(List((cat0, Percent(1)), (cat4, Percent(99))))
      fail
    } catch {
      case _: IllegalArgumentException => // ok
    }

  }

  /*
  @Test def testFunctionUpdated() {
    val grp2 = grp.updated(oc2, vals => Some(vals.map(perc => Percent(perc.value * 2)))).get

    assertEquals(Percent(20), grp2(cat0))
    assertEquals(Percent(40), grp2(cat1))
    assertEquals(Percent(60), grp2(cat2))
    assertEquals(Percent(40), grp2(cat3))
  }
  */
}