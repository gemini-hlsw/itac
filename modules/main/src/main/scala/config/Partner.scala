// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.config

import io.circe._

import edu.gemini.model.p1.immutable.NgoPartner
import edu.gemini.model.p1.immutable.Partners
import edu.gemini.model.p1.immutable.`package`.ExchangePartner
import edu.gemini.model.p1.immutable.`package`.LargeProgramPartner

sealed abstract case class Partner(
  value: Either[NgoPartner, Either[ExchangePartner, LargeProgramPartner.type]]
) {

  def id: String =
    value match {
      case Left(ngo)         => ngo.name
      case Right(Left(exch)) => exch.name
      case Right(Right(_))   => "LP"
    }

  def name: String =
    Partners.name(value.map(_.merge).merge) // Unsafe but this is how it's done in the P1 model

  def dummyConfig: PartnerConfig =
    PartnerConfig(Email.dummy, Percent(1.23), List(GN, GS))

}

object Partner {
  import itac.codec.tokens._

  def all: List[Partner] =
    NgoPartner.values.toList.map(ngo => new Partner(Left(ngo)) {}) ++
    ExchangePartner.values.toList.map(exch => new Partner(Right(Left(exch))) {}) ++
    List(new Partner(Right(Right(LargeProgramPartner))) {})

  def fromString(s: String): Either[String, Partner] =
    all.find(p => p.id == s || p.name == s).toRight(s"Unknown partner: $s")

  implicit val keyEncoderPartner: KeyEncoder[Partner] = KeyEncoder.instance(_.id)
  implicit val keyDecoderPartner: KeyDecoder[Partner] = KeyDecoder.instance(fromString(_).toOption)

  implicit val EncoderPartner: Encoder[Partner] = Encoder[String].contramap(_.id)
  implicit val DecoderPartner: Decoder[Partner] = Decoder[String].emap(fromString)

  implicit val encoderListPartner: Encoder[List[Partner]] = encodeTokens(_.id)
  implicit val decoderListPartner: Decoder[List[Partner]] = decodeTokens(fromString)


}
