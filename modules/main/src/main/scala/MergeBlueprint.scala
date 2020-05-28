// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac

import cats._
import cats.implicits._
import edu.gemini.model.p1.mutable._
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

/**
 * Mixin just to pull out the unavoidably verbose blueprint canonicalization logic.
 */
trait MergeBlueprint {
  import MergeBlueprintInstances._

  type Canonicalizer[A] = (A, Proposal) => A

  /**
   * Find the next id from a list of ids of the form prefix-<int>. Throws an exception if these
   * expectations aren't met.
   */
  protected def nextId(prefix: String, ids: Iterable[String]): String = {
    val i: Int =
      ids.map(_.split("-")).map { case Array(_, n) => n.toInt } match {
        case Nil => 0
        case ns  => ns.max + 1
      }
      s"$prefix-$i"
    }

  private def unNull[A >: Null](as: A*): List[A] =
    as.toList.filterNot(_ == null)

  def allBlueprints(p: Proposal): List[BlueprintBase] =
    p.getBlueprints.getFlamingos2OrGmosNOrGmosS.asScala.toList.flatMap {
      // copied list from generated Blueprints.java
      // each case is done by hand, new choices will be missed! ugh!
      // this means we could end up with a duplicate id. We will add 100 to new IDs just to be sure
      case c: Flamingos2BlueprintChoice => unNull(c.getImaging, c.getLongslit, c.getMos)
      case c: GmosNBlueprintChoice      => unNull(c.getIfu, c.getImaging, c.getLongslit, c.getLongslitNs, c.getMos)
      case c: GmosSBlueprintChoice      => unNull(c.getIfu, c.getIfuNs, c.getImaging, c.getLongslit, c.getLongslitNs, c.getMos)
      case c: GnirsBlueprintChoice      => unNull(c.getImaging, c.getSpectroscopy)
      case c: GsaoiBlueprintChoice      => unNull(c.getGsaoi)
      case c: GracesBlueprintChoice     => unNull(c.getGraces)
      case c: GpiBlueprintChoice        => unNull(c.getGpi)
      case c: KeckBlueprint             => List(c) // schema definition error?
      case c: IgrinsBlueprintChoice     => unNull(c.getIgrins)
      case c: MichelleBlueprintChoice   => unNull(c.getImaging, c.getSpectroscopy)
      case c: NiciBlueprintChoice       => unNull(c.getCoronagraphic, c.getStandard)
      case c: NifsBlueprintChoice       => unNull(c.getAo, c.getNonAo)
      case c: NiriBlueprintChoice       => unNull(c.getNiri)
      case c: PhoenixBlueprintChoice    => unNull(c.getPhoenix)
      case c: SubaruBlueprint           => List(c) // schema definition error?
      case c: DssiBlueprintChoice       => unNull(c.getDssi)
      case c: TexesBlueprintChoice      => unNull(c.getTexes)
      case c: TrecsBlueprintChoice      => unNull(c.getImaging, c.getSpectroscopy)
      case c: VisitorBlueprintChoice    => unNull(c.getVisitor)
      case c: AlopekeBlueprintChoice    => unNull(c.getAlopeke)
      case c: ZorroBlueprintChoice      => unNull(c.getZorro)
      case c => sys.error(s"MergeBlueprint.allBlueprints: unhandled class ${c.getClass.getName}")
    }

  private def nextBlueprintId(bps: List[BlueprintBase]): String =
    nextId("blueprint", bps.map(_.getId()))

  // just for keck and subaru!
  def canonicalizeBlueprintBaseWithoutChoice[A <: BlueprintBase : Eq : ClassTag]: Canonicalizer[A] = { (from, into) =>
    val coll = into.getBlueprints().getFlamingos2OrGmosNOrGmosS()
    val all  = allBlueprints(into).filter { o => implicitly[ClassTag[A]].runtimeClass.isInstance(o) } .map(_.asInstanceOf[A])
    all.find(from === _) match {
      case Some(bp) => bp
      case None =>
        from.setId(nextBlueprintId(all))
        coll.add(from)
        from
    }
  }

  def canonicalizeBlueprintBase[A <: BlueprintBase : Eq : ClassTag, C <: AnyRef : ClassTag](
    setter: (C, A) => Unit
  ): (A, Proposal) => A = { (from, into) =>
    val coll = into.getBlueprints().getFlamingos2OrGmosNOrGmosS()
    val all  = allBlueprints(into).filter { o => implicitly[ClassTag[A]].runtimeClass.isInstance(o) } .map(_.asInstanceOf[A])
    all.find(from === _) match {
      case Some(bp) => bp
      case None =>
        from.setId(nextBlueprintId(all))
        val choice = implicitly[ClassTag[C]].runtimeClass.newInstance().asInstanceOf[C]
        setter(choice, from)
        coll.add(choice)
        from
    }
  }

  val canonicalizeAlopekeBlueprint: Canonicalizer[AlopekeBlueprint] =
    canonicalizeBlueprintBase[AlopekeBlueprint, AlopekeBlueprintChoice](_ setAlopeke _)

  val canonicalizeDssiBlueprint: Canonicalizer[DssiBlueprint] =
    canonicalizeBlueprintBase[DssiBlueprint, DssiBlueprintChoice](_ setDssi _)

  val canonicalizeFlamingos2BlueprintImaging: Canonicalizer[Flamingos2BlueprintImaging] =
    canonicalizeBlueprintBase[Flamingos2BlueprintImaging, Flamingos2BlueprintChoice](_ setImaging _)

  val canonicalizeFlamingos2BlueprintLongslit: Canonicalizer[Flamingos2BlueprintLongslit] =
    canonicalizeBlueprintBase[Flamingos2BlueprintLongslit, Flamingos2BlueprintChoice](_ setLongslit _)

  val canonicalizeFlamingos2BlueprintMos: Canonicalizer[Flamingos2BlueprintMos] =
    canonicalizeBlueprintBase[Flamingos2BlueprintMos, Flamingos2BlueprintChoice](_ setMos _)

  val canonicalizeGmosNBlueprintIfu: Canonicalizer[GmosNBlueprintIfu] =
    canonicalizeBlueprintBase[GmosNBlueprintIfu, GmosNBlueprintChoice](_ setIfu _)

  val canonicalizeGmosNBlueprintImaging: Canonicalizer[GmosNBlueprintImaging] =
    canonicalizeBlueprintBase[GmosNBlueprintImaging, GmosNBlueprintChoice](_ setImaging _)

  val canonicalizeGmosNBlueprintLongslit: Canonicalizer[GmosNBlueprintLongslit] =
    canonicalizeBlueprintBase[GmosNBlueprintLongslit, GmosNBlueprintChoice](_ setLongslit _)

  val canonicalizeGmosNBlueprintLongslitNs: Canonicalizer[GmosNBlueprintLongslitNs] =
    canonicalizeBlueprintBase[GmosNBlueprintLongslitNs, GmosNBlueprintChoice](_ setLongslitNs _)

  val canonicalizeGmosNBlueprintMos: Canonicalizer[GmosNBlueprintMos] =
    canonicalizeBlueprintBase[GmosNBlueprintMos, GmosNBlueprintChoice](_ setMos _)

  val canonicalizeGmosSBlueprintIfu: Canonicalizer[GmosSBlueprintIfu] =
    canonicalizeBlueprintBase[GmosSBlueprintIfu, GmosSBlueprintChoice](_ setIfu _)

  val canonicalizeGmosSBlueprintIfuNs: Canonicalizer[GmosSBlueprintIfuNs] =
    canonicalizeBlueprintBase[GmosSBlueprintIfuNs, GmosSBlueprintChoice](_ setIfuNs _)

  val canonicalizeGmosSBlueprintImaging: Canonicalizer[GmosSBlueprintImaging] =
    canonicalizeBlueprintBase[GmosSBlueprintImaging, GmosSBlueprintChoice](_ setImaging _)

  val canonicalizeGmosSBlueprintLongslit: Canonicalizer[GmosSBlueprintLongslit] =
    canonicalizeBlueprintBase[GmosSBlueprintLongslit, GmosSBlueprintChoice](_ setLongslit _)

  val canonicalizeGmosSBlueprintLongslitNs: Canonicalizer[GmosSBlueprintLongslitNs] =
    canonicalizeBlueprintBase[GmosSBlueprintLongslitNs, GmosSBlueprintChoice](_ setLongslitNs _)

  val canonicalizeGmosSBlueprintMos: Canonicalizer[GmosSBlueprintMos] =
    canonicalizeBlueprintBase[GmosSBlueprintMos, GmosSBlueprintChoice](_ setMos _)

  val canonicalizeGnirsBlueprintImaging: Canonicalizer[GnirsBlueprintImaging] =
    canonicalizeBlueprintBase[GnirsBlueprintImaging, GnirsBlueprintChoice](_ setImaging _)

  val canonicalizeGnirsBlueprintSpectroscopy: Canonicalizer[GnirsBlueprintSpectroscopy] =
    canonicalizeBlueprintBase[GnirsBlueprintSpectroscopy, GnirsBlueprintChoice](_ setSpectroscopy _)

  val canonicalizeGpiBlueprint: Canonicalizer[GpiBlueprint] =
    canonicalizeBlueprintBase[GpiBlueprint, GpiBlueprintChoice](_ setGpi _)

  val canonicalizeGracesBlueprint: Canonicalizer[GracesBlueprint] =
    canonicalizeBlueprintBase[GracesBlueprint, GracesBlueprintChoice](_ setGraces _)

  val canonicalizeGsaoiBlueprint: Canonicalizer[GsaoiBlueprint] =
    canonicalizeBlueprintBase[GsaoiBlueprint, GsaoiBlueprintChoice](_ setGsaoi _)

  val canonicalizeIgrinsBlueprint: Canonicalizer[IgrinsBlueprint] =
    canonicalizeBlueprintBase[IgrinsBlueprint, IgrinsBlueprintChoice](_ setIgrins _)

  val canonicalizeKeckBlueprint: Canonicalizer[KeckBlueprint] =
    canonicalizeBlueprintBaseWithoutChoice[KeckBlueprint]

  val canonicalizeMichelleBlueprintImaging: Canonicalizer[MichelleBlueprintImaging] =
    canonicalizeBlueprintBase[MichelleBlueprintImaging, MichelleBlueprintChoice](_ setImaging _)

  val canonicalizeMichelleBlueprintSpectroscopy: Canonicalizer[MichelleBlueprintSpectroscopy] =
    canonicalizeBlueprintBase[MichelleBlueprintSpectroscopy, MichelleBlueprintChoice](_ setSpectroscopy _)

  val canonicalizeNiciBlueprintCoronagraphic: Canonicalizer[NiciBlueprintCoronagraphic] =
    canonicalizeBlueprintBase[NiciBlueprintCoronagraphic, NiciBlueprintChoice](_ setCoronagraphic _)

  val canonicalizeNiciBlueprintStandard: Canonicalizer[NiciBlueprintStandard] =
    canonicalizeBlueprintBase[NiciBlueprintStandard, NiciBlueprintChoice](_ setStandard _)

  val canonicalizeNifsBlueprint: Canonicalizer[NifsBlueprint] =
    canonicalizeBlueprintBase[NifsBlueprint, NifsBlueprintChoice](_ setNonAo _)

  val canonicalizeNifsBlueprintAo: Canonicalizer[NifsBlueprintAo] =
    canonicalizeBlueprintBase[NifsBlueprintAo, NifsBlueprintChoice](_ setAo _)

  val canonicalizeNiriBlueprint: Canonicalizer[NiriBlueprint] =
    canonicalizeBlueprintBase[NiriBlueprint, NiriBlueprintChoice](_ setNiri _)

  val canonicalizePhoenixBlueprint: Canonicalizer[PhoenixBlueprint] =
    canonicalizeBlueprintBase[PhoenixBlueprint, PhoenixBlueprintChoice](_ setPhoenix _)

  val canonicalizeSubaruBlueprint: Canonicalizer[SubaruBlueprint] =
    canonicalizeBlueprintBaseWithoutChoice[SubaruBlueprint]

  val canonicalizeTexesBlueprint: Canonicalizer[TexesBlueprint] =
    canonicalizeBlueprintBase[TexesBlueprint, TexesBlueprintChoice](_ setTexes _)

  val canonicalizeTrecsBlueprintImaging: Canonicalizer[TrecsBlueprintImaging] =
    canonicalizeBlueprintBase[TrecsBlueprintImaging, TrecsBlueprintChoice](_ setImaging _)

  val canonicalizeTrecsBlueprintSpectroscopy: Canonicalizer[TrecsBlueprintSpectroscopy] =
    canonicalizeBlueprintBase[TrecsBlueprintSpectroscopy, TrecsBlueprintChoice](_ setSpectroscopy _)

  val canonicalizeVisitorBlueprint: Canonicalizer[VisitorBlueprint] =
    canonicalizeBlueprintBase[VisitorBlueprint, VisitorBlueprintChoice](_ setVisitor _)

  val canonicalizeZorroBlueprint: Canonicalizer[ZorroBlueprint] =
    canonicalizeBlueprintBase[ZorroBlueprint, ZorroBlueprintChoice](_ setZorro _)

  /**
   * Find or destructively create matching `BlueprintBase` in `into`.
   */
  def canonicalize(from: BlueprintBase, into: Proposal): BlueprintBase =
    from match {
      // ls *Blueprint*.java | grep -v 'Base\|Choice\|Null\|Blueprints' | cut -f 1 -d .
      case bp: AlopekeBlueprint => canonicalizeAlopekeBlueprint(bp, into)
      case bp: DssiBlueprint => canonicalizeDssiBlueprint(bp, into)
      case bp: Flamingos2BlueprintImaging => canonicalizeFlamingos2BlueprintImaging(bp, into)
      case bp: Flamingos2BlueprintLongslit => canonicalizeFlamingos2BlueprintLongslit(bp, into)
      case bp: Flamingos2BlueprintMos => canonicalizeFlamingos2BlueprintMos(bp, into)
      case bp: GmosNBlueprintIfu => canonicalizeGmosNBlueprintIfu(bp, into)
      case bp: GmosNBlueprintImaging => canonicalizeGmosNBlueprintImaging(bp, into)
      case bp: GmosNBlueprintLongslit => canonicalizeGmosNBlueprintLongslit(bp, into)
      case bp: GmosNBlueprintLongslitNs => canonicalizeGmosNBlueprintLongslitNs(bp, into)
      case bp: GmosNBlueprintMos => canonicalizeGmosNBlueprintMos(bp, into)
      case bp: GmosSBlueprintIfu => canonicalizeGmosSBlueprintIfu(bp, into)
      case bp: GmosSBlueprintIfuNs => canonicalizeGmosSBlueprintIfuNs(bp, into)
      case bp: GmosSBlueprintImaging => canonicalizeGmosSBlueprintImaging(bp, into)
      case bp: GmosSBlueprintLongslit => canonicalizeGmosSBlueprintLongslit(bp, into)
      case bp: GmosSBlueprintLongslitNs => canonicalizeGmosSBlueprintLongslitNs(bp, into)
      case bp: GmosSBlueprintMos => canonicalizeGmosSBlueprintMos(bp, into)
      case bp: GnirsBlueprintImaging => canonicalizeGnirsBlueprintImaging(bp, into)
      case bp: GnirsBlueprintSpectroscopy => canonicalizeGnirsBlueprintSpectroscopy(bp, into)
      case bp: GpiBlueprint => canonicalizeGpiBlueprint(bp, into)
      case bp: GracesBlueprint => canonicalizeGracesBlueprint(bp, into)
      case bp: GsaoiBlueprint => canonicalizeGsaoiBlueprint(bp, into)
      case bp: IgrinsBlueprint => canonicalizeIgrinsBlueprint(bp, into)
      case bp: KeckBlueprint => canonicalizeKeckBlueprint(bp, into)
      case bp: MichelleBlueprintImaging => canonicalizeMichelleBlueprintImaging(bp, into)
      case bp: MichelleBlueprintSpectroscopy => canonicalizeMichelleBlueprintSpectroscopy(bp, into)
      case bp: NiciBlueprintCoronagraphic => canonicalizeNiciBlueprintCoronagraphic(bp, into)
      case bp: NiciBlueprintStandard => canonicalizeNiciBlueprintStandard(bp, into)
      case bp: NifsBlueprint => canonicalizeNifsBlueprint(bp, into)
      case bp: NifsBlueprintAo => canonicalizeNifsBlueprintAo(bp, into)
      case bp: NiriBlueprint => canonicalizeNiriBlueprint(bp, into)
      case bp: PhoenixBlueprint => canonicalizePhoenixBlueprint(bp, into)
      case bp: SubaruBlueprint => canonicalizeSubaruBlueprint(bp, into)
      case bp: TexesBlueprint => canonicalizeTexesBlueprint(bp, into)
      case bp: TrecsBlueprintImaging => canonicalizeTrecsBlueprintImaging(bp, into)
      case bp: TrecsBlueprintSpectroscopy => canonicalizeTrecsBlueprintSpectroscopy(bp, into)
      case bp: VisitorBlueprint => canonicalizeVisitorBlueprint(bp, into)
      case bp: ZorroBlueprint => canonicalizeZorroBlueprint(bp, into)
      case _ => (into, sys.error("blah") : BlueprintBase)._2
    }

}

