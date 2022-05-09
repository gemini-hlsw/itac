// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.tac.qengine.p1.io

import edu.gemini.model.p1.{immutable => im}
import edu.gemini.model.p1.{mutable => m}
import edu.gemini.spModel.core.Site
import edu.gemini.tac.qengine.p1._
import edu.gemini.tac.qengine.p1.io.ObservationIo.{ GroupedObservations, BandChoice }
import edu.gemini.tac.qengine.p1.io.ObservationIo.BandChoice.{ Band124 => B1_2, Band3 => B3 }
import edu.gemini.tac.qengine.util.Time

import org.junit._
import Assert._

import scalaz._

object ObservationIoTest {
  val when = System.currentTimeMillis()

  val nifsAo  = im.NifsBlueprintAo(im.AltairLGS(pwfs1 = true), im.NifsOccultingDisk.OD_NONE, im.NifsDisperser.Z)
  val twoHour = im.TimeAmount(2.0, im.TimeUnit.HR)
}

import ObservationIoTest._

class ObservationIoTest {

  @Test def readSiderealTarget(): Unit = {
    val p = new ProposalFixture()
    val t = ObservationIo.target(p.siderealTarget, when)

    assertTrue(t.name.contains("BiffStar"))
    assertEquals(152.8000000, t.ra.mag,  0.000001)
    assertEquals(-20.5111111, t.dec.mag, 0.000001)
  }

  @Test def readTooTarget(): Unit = {
    val imt = im.TooTarget.empty
    val t   = ObservationIo.target(imt, when)
    assertEquals(0, t.ra.mag,  0.000001)
    assertEquals(0, t.dec.mag, 0.000001)
  }

  @Test def readConditions(): Unit = {
    val imc = im.Condition(
      None,
      m.CloudCover.cc50,
      m.ImageQuality.iq70,
      m.SkyBackground.sb80,
      m.WaterVapor.wv100
    )
    val c = ObservationIo.conditions(imc)
    assertEquals(CloudCover.CC50,    c.cc)
    assertEquals(ImageQuality.IQ70,  c.iq)
    assertEquals(SkyBackground.SB80, c.sb)
    assertEquals(WaterVapor.WVAny,   c.wv)
  }

  @Test def readTime(): Unit = {
    ObservationIo.time((new ProposalFixture).observation) match {
      case Success(t) => assertEquals(t.value, 1.0, 0.000001)
      case _          => fail("Expected 1 hour")
    }
  }

  @Test def readNegativeTime(): Unit = {
    val minusOne = im.TimeAmount(-1.0, im.TimeUnit.HR)

    val p = new ProposalFixture {
      override def oneHour = minusOne
    }

    ObservationIo.time(p.observation) match {
      case Failure(NonEmptyList(msg, _)) => assertEquals(s"Negative observation amount: ${minusOne.hours} hours", msg)
      case _                             => fail("Expected failure, negative time")
    }
  }

  @Test def readNoLgs(): Unit = {
    ObservationIo.lgs((new ProposalFixture).observation) match {
      case Success(false) => // ok
      case _              => fail("Expected no LGS")
    }
  }

  @Test def readLgs(): Unit = {
    val p = new ProposalFixture {
      override def blueprint: im.BlueprintBase = nifsAo
    }

    ObservationIo.lgs(p.observation) match {
      case Success(true) => // ok
      case _             => fail("Expected LGS")
    }
  }

  private def missing(expected: String)(p: ProposalFixture): Unit =
    ObservationIo.read(p.observation, when) match {
      case Failure(NonEmptyList(msg, _)) => assertEquals(expected, msg)
      case _                             => fail(s"Expected: $expected")
    }

  @Test def readNoBlueprint(): Unit = {
    // With the new model if there is no blueprint the calculation for time
    // fails. When an observation is read the time is read before the blueprint.
    missing(ObservationIo.MISSING_TIME) {
      new ProposalFixture {
        override def observation = im.Observation(
          None,
          Some(conditions),
          Some(siderealTarget),
          im.Band.BAND_1_2,
          Some(oneHour)
        )
      }
    }
  }

  @Test def readNoConditions(): Unit = {
    missing(ObservationIo.MISSING_CONDITIONS) {
      new ProposalFixture {
        override def observation = im.Observation(
          Some(blueprint),
          None,
          Some(siderealTarget),
          im.Band.BAND_1_2,
          Some(oneHour)
        )
      }
    }
  }

  @Test def readNoTarget(): Unit = {
    missing(ObservationIo.MISSING_TARGET) {
      new ProposalFixture {
        override def observation = im.Observation(
          Some(blueprint),
          Some(conditions),
          None,
          im.Band.BAND_1_2,
          Some(oneHour)
        )
      }
    }
  }

  @Test def readNoTime(): Unit = {
    missing(ObservationIo.MISSING_TIME) {
      new ProposalFixture {
        override def observation = im.Observation(
          Some(blueprint),
          Some(conditions),
          Some(siderealTarget),
          im.Band.BAND_1_2,
          None
        )
      }
    }
  }

  @Test def readMissingMultiple(): Unit = {
    val p = new ProposalFixture {
      override def observation = im.Observation(
        Some(blueprint),
        None,
        Some(siderealTarget),
        im.Band.BAND_1_2,
        None
      )
    }

    ObservationIo.read(p.observation, when) match {
      case Failure(NonEmptyList(msg0, msg1)) =>
        assertEquals(Set(ObservationIo.MISSING_CONDITIONS, ObservationIo.MISSING_TIME), Set(msg0, msg1.headOption.get))
      case _                                 => fail("Expected missing time and conditions")
    }
  }

  @Test def readMissingObservations(): Unit = {
    val p = new ProposalFixture {
      override def observations = Nil
    }

    ObservationIo.readAllAndGroup(p.proposal, when) match {
      case Failure(NonEmptyList(msg, _)) => assertEquals(ObservationIo.MISSING_OBSERVATIONS, msg)
      case _                             => fail("Expected missing observations")
    }
  }

  @Test def readOneObservation(): Unit = {
    ObservationIo.readAllAndGroup((new ProposalFixture).proposal, when) match {
      case Success(NonEmptyList((Site.GS, B1_2, NonEmptyList(obs, _)), _)) => // ok
      case _ => fail("Expected a single GS Band1/2 observation")
    }
  }

  private def mapObsGroups(grps: GroupedObservations): Map[(Site, BandChoice), NonEmptyList[Observation]] =
    grps.list.toList.map { case (s, c, os) => (s, c) -> os }.toMap

  @Test def readMultiSiteObservations(): Unit = {
    val p = new ProposalFixture {
      private def nifsObservation = im.Observation(
        Some(nifsAo),
        Some(conditions),
        Some(siderealTarget),
        im.Band.BAND_1_2,
        Some(oneHour)
      )
      override def observations = List(observation, nifsObservation)
    }

    ObservationIo.readAllAndGroup(p.proposal, when) match {
      case Success(nel) =>
        val m = mapObsGroups(nel)
        val os1 = m((Site.GS, B1_2))
        val os2 = m((Site.GN, B1_2))
        assertEquals(2, m.size)
        assertEquals(false, os1.head.lgs)
        assertEquals(true,  os2.head.lgs)
        assertEquals(1, os1.size)
        assertEquals(1, os2.size)
      case _ => fail("Expected two groups")
    }
  }

  @Test def readMultiBandObservations(): Unit = {
    val p = new ProposalFixture {
      private def b3Observation = im.Observation(
        Some(blueprint),
        Some(conditions),
        Some(siderealTarget),
        im.Band.BAND_3,
        Some(oneHour)
      )
      override def observations = List(observation, b3Observation)
    }

    ObservationIo.readAllAndGroup(p.proposal, when) match {
      case Success(nel) =>
        val m = mapObsGroups(nel)
        val os1 = m((Site.GS, B1_2))
        val os2 = m((Site.GS, B3))
        assertEquals(2, m.size)
        assertEquals(1, os1.size)
        assertEquals(1, os2.size)
      case _ => fail("Expected two groups")
    }
  }

  @Test def readMultiObservations(): Unit = {
    val p = new ProposalFixture {
      private def observation2 = im.Observation(
        Some(blueprint),
        Some(conditions),
        Some(siderealTarget),
        im.Band.BAND_1_2,
        Some(twoHour)
      )

      private def b3Observation = im.Observation(
        Some(blueprint),
        Some(conditions),
        Some(siderealTarget),
        im.Band.BAND_3,
        Some(oneHour)
      )

      private def nifsObservation = im.Observation(
        Some(nifsAo),
        Some(conditions),
        Some(siderealTarget),
        im.Band.BAND_1_2,
        Some(oneHour)
      )

      override def observations = List(observation, b3Observation, observation2, nifsObservation)
    }

    ObservationIo.readAllAndGroup(p.proposal, when) match {
      case Success(nel) =>
        val m = mapObsGroups(nel)
        val os1 = m((Site.GS, B1_2))
        val os2 = m((Site.GS, B3))
        val os3 = m((Site.GN, B1_2))
        assertEquals(3, m.size)
        assertEquals(2, os1.size)
        assertEquals(1, os2.size)
        assertEquals(1, os3.size)
        os1.list.toList.map(_.time) match {
          case List(t1, t2) =>
            assertEquals(Time.hours(1), t1)
            assertEquals(Time.hours(2), t2)
          case _ => fail("Expected one hour then two hour")
        }
      case _ => fail("Expected two groups")
    }
  }
}
