// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats.Hash
import cats.implicits._
import edu.gemini.model.p1.immutable._
import edu.gemini.spModel.core.Coordinates
import edu.gemini.spModel.core.Magnitude
import edu.gemini.spModel.core.MagnitudeBand
import edu.gemini.spModel.core.MagnitudeSystem

/** Provides a stable hash for `Observation`. */
object ObservationDigest {

  /** Java enumerations are hashed by name. */
  implicit def hashJavaEnum[A <: Enum[A]]: Hash[A] =
    Hash.by { e => e.name }

  /** TimeAmount is hashed by duration in hours. */
  implicit val HashTimeAmount: Hash[TimeAmount] =
    Hash.by { a => a.toHours.value }

  implicit val HashObservationTimes: Hash[ObservationTimes] =
    Hash.by { t => (t.partTime, t.progTime) }

  implicit val HashCondition: Hash[Condition] =
    Hash.by { c => (c.cc, c.iq, c.maxAirmass, c.sb, c.wv) }

  implicit val HashGuidingEstimation: Hash[GuidingEstimation] =
    Hash.by { e => e.perc }

  implicit val HashTargetVisibility: Hash[TargetVisibility] =
    Hash.by { v => v.toString } // these are case objects

  implicit val HashObservationMeta: Hash[ObservationMeta] =
    Hash.by { m => (m.gsa, m.guiding, m.visibility) }

  implicit val HashCoordinates: Hash[Coordinates] =
    Hash.by { cs => (cs.ra.formatHMS, cs.dec.formatDMS) }

  implicit val HashProperMotion: Hash[ProperMotion] =
    Hash.by { pm => (pm.deltaRA, pm.deltaDec) }

  implicit val HashMagnitudeBand: Hash[MagnitudeBand] =
    Hash.by { mb => mb.name }

  implicit val HashMagnitudeSystem: Hash[MagnitudeSystem] =
    Hash.by { ms => ms.name }

  implicit val HashMagnitude: Hash[Magnitude] =
    Hash.by { m => (m.band, m.error, m.system, m.value) }

  implicit val HashEphemerisElement: Hash[EphemerisElement] =
    Hash.by { e => (e.coords, e.magnitude, e.validAt) }

  implicit val HashTarget: Hash[Target] =
    Hash.by {
      // N.B. ignore the UUID, which is assigned by the deserializer
      case TooTarget(_, n)                       => (n).hash
      case SiderealTarget(_, n, cs, e, pm, mags) => (n, cs, e, pm, mags).hash
      case NonSiderealTarget(_, n, eph, e)       => (n, eph, e).hash
    }

  implicit val HashAltair: Hash[Altair] =
    Hash.by {
      case AltairNone                     => 0
      case AltairLGS(pwfs1, aowfs, oiwfs) => (pwfs1, aowfs, oiwfs).hash
      case AltairNGS(fieldLens)           => fieldLens.hash
    }

  implicit val HashSite: Hash[Site] =
    Hash.by { s => s.abbreviation }

  /** Hash blueprints by instrument name + config. */
  implicit val HashBlueprintBase: Hash[BlueprintBase] =
    Hash.by[BlueprintBase, Int] {
      case b: AlopekeBlueprint              => (b.name, b.mode).hash
      case b: Flamingos2BlueprintImaging    => (b.name, b.filters).hash
      case b: Flamingos2BlueprintLongslit   => (b.name, b.disperser, b.filters, b.fpu).hash
      case b: Flamingos2BlueprintMos        => (b.name, b.disperser, b.filters, b.preImaging).hash
      case b: GmosSBlueprintIfu             => (b.name, b.disperser, b.filter, b.fpu).hash
      case b: GmosSBlueprintIfuNs           => (b.name, b.disperser, b.filter, b.fpu).hash
      case b: GmosSBlueprintImaging         => (b.name, b.filters).hash
      case b: GmosSBlueprintLongslit        => (b.name, b.disperser, b.filter, b.fpu).hash
      case b: GmosSBlueprintLongslitNs      => (b.name, b.disperser, b.filter, b.fpu).hash
      case b: GmosSBlueprintMos             => (b.name, b.disperser, b.filter, b.fpu, b.nodAndShuffle, b.preImaging).hash
      case b: GmosNBlueprintIfu             => (b.name, b.altair, b.disperser, b.filter, b.fpu).hash
      case b: GmosNBlueprintImaging         => (b.name, b.altair, b.filters).hash
      case b: GmosNBlueprintLongslit        => (b.name, b.altair, b.disperser, b.filter, b.fpu).hash
      case b: GmosNBlueprintLongslitNs      => (b.name, b.altair, b.disperser, b.filter, b.fpu).hash
      case b: GmosNBlueprintMos             => (b.name, b.altair, b.disperser, b.filter, b.fpu, b.nodAndShuffle, b.preImaging).hash
      case b: GnirsBlueprintImaging         => (b.name, b.altair, b.filter, b.pixelScale).hash
      case b: GnirsBlueprintSpectroscopy    => (b.name, b.altair, b.centralWavelength, b.crossDisperser, b.disperser, b.fpu, b.pixelScale).hash
      case b: GsaoiBlueprint                => (b.name, b.filters).hash
      case b: GracesBlueprint               => (b.name, b.fiberMode, b.readMode).hash
      case b: GpiBlueprint                  => (b.name, b.disperser, b.observingMode).hash
      case b: IgrinsBlueprint               => (b.name).hash
      case b: MichelleBlueprintImaging      => (b.name, b.filters, b.polarimetry).hash
      case b: MichelleBlueprintSpectroscopy => (b.name, b.disperser, b.fpu).hash
      case b: NiciBlueprintCoronagraphic    => (b.name, b.blueFilters, b.dichroic, b.fpm, b.redFilters).hash
      case b: NiciBlueprintStandard         => (b.name, b.blueFilters, b.dichroic, b.redFilters).hash
      case b: NifsBlueprint                 => (b.name, b.disperser).hash
      case b: NifsBlueprintAo               => (b.name, b.altair, b.disperser, b.occultingDisk).hash
      case b: NiriBlueprint                 => (b.name, b.altair, b.camera, b.filters).hash
      case b: PhoenixBlueprint              => (b.name, b.filter, b.fpu, b.site).hash
      case b: DssiBlueprint                 => (b.name, b.site).hash
      case b: TexesBlueprint                => (b.name, b.disperser, b.site).hash
      case b: TrecsBlueprintImaging         => (b.name, b.filters).hash
      case b: TrecsBlueprintSpectroscopy    => (b.name, b.disperser, b.fpu).hash
      case b: ZorroBlueprint                => (b.name, b.mode).hash
      case b: SubaruBlueprint               => (b.name, b.customName, b.instrument).hash
      case b: KeckBlueprint                 => (b.name, b.instrument).hash
      case b: VisitorBlueprint              => (b.name, b.customName, b.site).hash
      case b                                => sys.error(s"Unknown blueprint type: ${b.name}")
    }

  /** Observations are hashed by all members other than the `enabled` bit. */
  implicit val HashObservation: Hash[Observation] =
    Hash.by { o =>
      (
        o.band,
        o.blueprint,
        o.condition,
        // o.enabled, <-- explicitly ignoring this
        o.meta,
        o.progTime,
        o.realTarget
      )
    }

  /** A zero-padded 8-character hash digest of the given observation. */
  def digest(o: Observation): String =
    ("00000000" + o.hash.toHexString).takeRight(8)

}