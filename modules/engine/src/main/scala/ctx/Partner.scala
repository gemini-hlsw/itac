package edu.gemini.tac.qengine.ctx

import edu.gemini.spModel.core.Site
import edu.gemini.model.p1.mutable.{ NgoPartner, ExchangePartner }
import edu.gemini.model.p1.immutable.LargeProgramPartner

sealed abstract class Partner(
  val id: String,
  val sites: Set[Site]
) {
  final override def toString = id
}

object Partner {
  import Site.{ GN, GS }

  case object AR     extends Partner("AR",     Set(GN, GS))
  case object AU     extends Partner("AU",     Set(GN, GS))
  case object BR     extends Partner("BR",     Set(GN, GS))
  case object CA     extends Partner("CA",     Set(GN, GS))
  case object CFH    extends Partner("CFH",    Set(GN, GS))
  case object CL     extends Partner("CL",     Set(GS))
  case object KECK   extends Partner("KECK",   Set(GN, GS))
  case object KR     extends Partner("KR",     Set(GN, GS))
  case object LP     extends Partner("LP",     Set(GN, GS))
  case object SUBARU extends Partner("SUBARU", Set(GN, GS))
  case object UH     extends Partner("UH",     Set(GN))
  case object US     extends Partner("US",     Set(GN, GS))

  def all: List[Partner] =
    List(AR, AU, BR, CA, CFH, CL, KECK, KR, LP, SUBARU, UH, US)

  def fromString(id: String): Option[Partner] =
    all.find(_.id == id)

  def fromPhase1(
    value: Either[NgoPartner, Either[ExchangePartner, LargeProgramPartner.type]]
  ): Partner =
    value match {
      case Left(NgoPartner.CA) => CA
      case Left(NgoPartner.BR) => BR
      case Left(NgoPartner.KR) => KR
      case Left(NgoPartner.AU) => AU
      case Left(NgoPartner.US) => US
      case Left(NgoPartner.AR) => AR
      case Left(NgoPartner.CL) => CL
      case Left(NgoPartner.UH) => UH
      case Right(Left(ExchangePartner.KECK)) => KECK
      case Right(Left(ExchangePartner.SUBARU)) => SUBARU
      case Right(Left(ExchangePartner.CFH)) => CFH
      case Right(Right(LargeProgramPartner)) => LP
    }

  /**
   * Creates a map with entries for all Partners according to the value
   * returned by the supplied function.
   */
  def mkMap[T](values: List[Partner], f: Partner => T): Map[Partner, T] =
    values.map(p => p -> f(p)).toMap

  // Completes the given PartialFunction by returning a default value for any
  // Partner for which pf is not defined.
  private def complete[T](pf: PartialFunction[Partner, T], default: T): Partner => T =
    p => pf.lift(p).getOrElse(default)

  /**
   * Creates a map with entries for all Partners according to the value
   * returned by the supplied partial function, using the supplied default
   * for any values not in pf's domain.
   */
  def mkMap[T](values: List[Partner], pf: PartialFunction[Partner, T], default: T): Map[Partner, T] =
    mkMap(values, complete(pf, default))

}
