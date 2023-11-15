// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats._
import cats.implicits._
import edu.gemini.model.p1.mutable._
import java.{util => ju}
import scala.jdk.CollectionConverters._

/**
 * Eq instances for determining whether mutable blueprints are equivalent. Super sketchy, only use
 * locally.
 */
object MergeBlueprintInstances {

  implicit def eqJavaEnum[E <: Enum[E]]: Eq[E] =
    Eq.by(_.ordinal)

  implicit def eqJavaBoolean: Eq[java.lang.Boolean] =
    Eq.by(_.booleanValue)

  // LOCALLY ONLY we compare lists of enums as equal if they contain the same elements in any order
  implicit def eqJavaListEnum[E <: Enum[E]]: Eq[ju.List[E]] =
    Eq.by(_.asScala.toSet)

  implicit val EqAltairChoice: Eq[AltairChoice] =
    Eq.by(a =>
      Option(a.getLgs).map(a => Left((a.isAowfs, a.isOiwfs, a.isPwfs1))) orElse
      Option(a.getNgs).map(a => Right(a.isFieldLens)) // or else it has to be None
    )

  implicit val EqAlopekeBlueprint: Eq[AlopekeBlueprint] =
    Eq.by(b => (b.getMode))

  implicit val EqDssiBlueprint: Eq[DssiBlueprint] =
    Eq.by(b => (b.getSite))

  implicit val EqFlamingos2BlueprintImaging: Eq[Flamingos2BlueprintImaging] =
    Eq.by(b => (b.getFilter))

  implicit val EqFlamingos2BlueprintLongslit: Eq[Flamingos2BlueprintLongslit] =
    Eq.by(b => (b.getFpu, b.getDisperser, b.getFilter))

  implicit val EqFlamingos2BlueprintMos: Eq[Flamingos2BlueprintMos] =
    Eq.by(b => (b.getDisperser, b.getFilter))

  implicit val EqGhostBlueprint: Eq[GhostBlueprint] =
    Eq.by(b => (b.getResolutionMode, b.getTargetMode))

  implicit val EqGmosNBlueprintIfu: Eq[GmosNBlueprintIfu] =
    Eq.by(b => (b.getFpu, b.getAltair, b.getDisperser, b.getFilter))

  implicit val EqGmosNBlueprintImaging: Eq[GmosNBlueprintImaging] =
    Eq.by(b => (b.getFilter, b.getAltair))

  implicit val EqGmosNBlueprintLongslit: Eq[GmosNBlueprintLongslit] =
    Eq.by(b => (b.getFpu, b.getAltair, b.getDisperser, b.getFilter))

  implicit val EqGmosNBlueprintLongslitNs: Eq[GmosNBlueprintLongslitNs] =
    Eq.by(b => (b.getFpu, b.getAltair, b.getDisperser, b.getFilter))

  implicit val EqGmosNBlueprintMos: Eq[GmosNBlueprintMos] =
    Eq.by(b => (b.getFpu, b.getAltair, b.getDisperser, b.getFilter, b.isNodAndShuffle(), b.isPreimaging()))

  implicit val EqGmosSBlueprintIfu: Eq[GmosSBlueprintIfu] =
    Eq.by(b => (b.getFpu, b.getDisperser, b.getFilter))

  implicit val EqGmosSBlueprintIfuNs: Eq[GmosSBlueprintIfuNs] =
    Eq.by(b => (b.getFpu, b.getDisperser, b.getFilter))

  implicit val EqGmosSBlueprintImaging: Eq[GmosSBlueprintImaging] =
    Eq.by(b => (b.getFilter))

  implicit val EqGmosSBlueprintLongslit: Eq[GmosSBlueprintLongslit] =
    Eq.by(b => (b.getFpu, b.getDisperser, b.getFilter))

  implicit val EqGmosSBlueprintLongslitNs: Eq[GmosSBlueprintLongslitNs] =
    Eq.by(b => (b.getFpu, b.getDisperser, b.getFilter))

  implicit val EqGmosSBlueprintMos: Eq[GmosSBlueprintMos] =
    Eq.by(b => (b.getFpu(), b.getDisperser(), b.getFilter(), b.isNodAndShuffle(), b.isPreimaging()))

  implicit val EqGnirsBlueprintImaging: Eq[GnirsBlueprintImaging] =
    Eq.by(b => (b.getFilter(), b.getAltair()))

  implicit val EqGnirsBlueprintSpectroscopy: Eq[GnirsBlueprintSpectroscopy] =
    Eq.by(b => (b.getCentralWavelength(), b.getCrossDisperser(), b.getDisperser(), b.getFpu(), b.getAltair()))

  implicit val EqGpiBlueprint: Eq[GpiBlueprint] =
    Eq.by(b => (b.getDisperser(), b.getObservingMode()))

  implicit val EqGracesBlueprint: Eq[GracesBlueprint] =
    Eq.by(b => (b.getFiberMode(), b.getReadMode()))

  implicit val EqGsaoiBlueprint: Eq[GsaoiBlueprint] =
    Eq.by(b => (b.getFilter()))

  implicit val EqIgrinsBlueprint: Eq[IgrinsBlueprint] =
    Eq.allEqual

  implicit val EqKeckBlueprint: Eq[KeckBlueprint] =
    Eq.by(b => (b.getInstrument()))

  implicit val EqMichelleBlueprintImaging: Eq[MichelleBlueprintImaging] =
    Eq.by(b => (b.getFilter(), b.getPolarimetry()))

  implicit val EqMichelleBlueprintSpectroscopy: Eq[MichelleBlueprintSpectroscopy] =
    Eq.by(b => (b.getDisperser(), b.getFpu()))

  implicit val EqNiciBlueprintCoronagraphic: Eq[NiciBlueprintCoronagraphic] =
    Eq.by(b => (b.getFpm(), b.getBlueFilter(), b.getDichroic()))

  implicit val EqNiciBlueprintStandard: Eq[NiciBlueprintStandard] =
    Eq.by(b => (b.getBlueFilter(), b.getDichroic()))

  implicit val EqNifsBlueprint: Eq[NifsBlueprint] =
    Eq.by(b => (b.getDisperser()))

  implicit val EqNifsBlueprintAo: Eq[NifsBlueprintAo] =
    Eq.by(b => (b.getAltair(), b.getOccultingDisk(), b.getDisperser()))

  implicit val EqNiriBlueprint: Eq[NiriBlueprint] =
    Eq.by(b => (b.getAltair(), b.getCamera(), b.getFilter()))

  implicit val EqPhoenixBlueprint: Eq[PhoenixBlueprint] =
    Eq.by(b => (b.getFilter(), b.getFpu(), b.getSite()))

  implicit val EqSubaruBlueprint: Eq[SubaruBlueprint] =
    Eq.by(b => (b.getCustomName(), b.getInstrument()))

  implicit val EqTexesBlueprint: Eq[TexesBlueprint] =
    Eq.by(b => (b.getDisperser(), b.getSite()))

  implicit val EqTrecsBlueprintImaging: Eq[TrecsBlueprintImaging] =
    Eq.by(b => (b.getFilter()))

  implicit val EqTrecsBlueprintSpectroscopy: Eq[TrecsBlueprintSpectroscopy] =
    Eq.by(b => (b.getDisperser(), b.getFpu()))

  implicit val EqVisitorBlueprint: Eq[VisitorBlueprint] =
    Eq.by(b => (b.getCustomName(), b.getSite()))

  implicit val EqZorroBlueprint: Eq[ZorroBlueprint] =
    Eq.by(b => (b.getMode()))

  implicit val EqMaroonXBlueprint: Eq[MaroonXBlueprint] =
    Eq.allEqual


}