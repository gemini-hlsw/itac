// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats.implicits._
import edu.gemini.tac.qengine.p1.Proposal
import itac.util.OneOrTwo
import edu.gemini.tac.qengine.p1.Observation
import gsp.math.HourAngle
import gsp.math.Angle
import gsp.math.Declination

case class Summary(slices: OneOrTwo[Proposal]) {
  import Summary.{ BandedObservation, Slice }

  val siteSummaries: OneOrTwo[Slice] =
    slices.map(Slice(_))

  // As a sanity check let's glance at the proposals. There are many other things we assumed to be
  // equal but if this checks out there would have to be something very wrong elswehere for the
  // slices to not correspond.
  slices.fold(_ => (), (a, b) => {
    require(a.site       != b.site,       "sites must differ")
    require(a.p1proposal == b.p1proposal, "underlying p1 proposals must be equal")
  })

  val reference = slices.fst.ntac.reference
  val mode      = slices.fst.mode
  val pi        = slices.fst.piName.orEmpty
  val partner   = slices.fst.ntac.partner
  val award     = slices.fst.ntac.awardedTime
  val rank      = slices.fst.ntac.ranking.num.orEmpty
  val too       = slices.fst.too

  def yaml(implicit ev: Ordering[BandedObservation]) =
    f"""|
        |Reference: ${reference}
        |Mode:      ${mode}
        |PI:        ${pi}
        |Partner:   ${partner.fullName}
        |Award:     ${award.toHours.value}%1.1f
        |Rank:      ${rank}%1.1f
        |ToO:       ${too}
        |
        |Observations:
        |${siteSummaries.foldMap(_.yaml)}
        |""".stripMargin

}

object Summary {

  // no time to make band a proper type, sorry
  final case class BandedObservation(band: String, obs: Observation)

  // Slight convenience
  private implicit class ObservationOps(o: Observation) {
    def ra:  HourAngle = Angle.hourAngle.get(Angle.fromDoubleDegrees(o.target.ra.mag))
    def dec: Angle     = Angle.fromDoubleDegrees(o.target.dec.mag)
  }

  // A slice, north or south.
  final case class Slice(proposal: Proposal) {

    private def bandedObservations(implicit ev: Ordering[BandedObservation]): List[BandedObservation] = {
      proposal.obsList          .map(BandedObservation("B1/2", _)) ++
      proposal.band3Observations.map(BandedObservation("B3",   _))
    } .sorted

    private def obsYaml(bo: BandedObservation): String = {
      val o     = bo.obs
      val id    = ObservationDigest.digest(o.p1Observation)
      val ra    = HourAngle.HMS(o.ra).format
      val dec   = Declination.fromAngle.getOption(o.dec).map(Declination.fromStringSignedDMS.reverseGet).getOrElse(sys.error(s"unpossible: invalid declination for $bo"))
      val conds = f"${o.conditions.cc}%-5s ${o.conditions.iq}%-5s ${o.conditions.sb}%-5s ${o.conditions.wv}%-5s "
      val hrs   = o.time.toHours.value
      f"  - $id  ${bo.band}%-4s  $hrs%5.1fh  $conds  $ra%16s  $dec%16s  ${o.target.name.orEmpty}\n"
    }

    def yaml(implicit ev: Ordering[BandedObservation]): String =
      f"  ${proposal.site.abbreviation}:\n${bandedObservations.foldMap(obsYaml)}\n"

  }

}

