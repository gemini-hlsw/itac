// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.config

import io.circe._
import io.circe.generic.semiauto._
import edu.gemini.tac.qengine.api.config.Shutdown
import java.{util => ju}
import java.time.LocalDate
import edu.gemini.tac.qengine.api.config.ConditionsBin
import edu.gemini.tac.qengine.api.config.ConditionsBinGroup
import itac.config.Common.EmailConfig
import edu.gemini.tac.qengine.ctx.Partner
import edu.gemini.tac.qengine.api.config.PartnerSequence

final case class Common(
  semester: Semester,
  shutdown: PerSite[List[LocalDateRange]],
  sequence: PerSite[List[Partner]],
  conditionsBins: List[ConditionsBin[Percent]],
  emailConfig: EmailConfig
) { self =>

  object engine {

    def partnerSequence(site: Site): PartnerSequence =
      new PartnerSequence {
        def sequence = self.sequence.forSite(site).to(LazyList) #::: sequence
        override def toString = s"PartnerSequence(...)"
      }

    def shutdowns(site: Site): List[Shutdown] =
      shutdown.forSite(site).map { ldr =>
        // Turn a LocalDate to a ju.Date at noon at `site`.
        def date(ldt: LocalDate): ju.Date = {
          val zid = site.timezone.toZoneId
          val zdt = ldt.atStartOfDay(zid).plusHours(12L)
          new ju.Date(zdt.toEpochSecond * 1000)
        }
        Shutdown(site, date(ldr.start), date(ldr.end))
      }

    lazy val conditionsBins: ConditionsBinGroup[Percent] =
      ConditionsBinGroup.of(Common.this.conditionsBins)

  }

}

object Common {
  import itac.codec.all._

  implicit val encoderListPartner: Encoder[List[Partner]] = encodeTokens(_.id)
  implicit val decoderListPartner: Decoder[List[Partner]] = decodeTokens(s => Partner.fromString(s).toRight(s"Invalid partner: $s"))

  implicit val encoderCommon: Encoder[Common] = deriveEncoder
  implicit val decoderCommon: Decoder[Common] = deriveDecoder

  case class EmailConfig(
    deadline: LocalDate,
    instructionsURL: String,
    eavesdroppingURL: String,
    hashKey: String
  )
  object EmailConfig {
    implicit val encoderEmailConfig: Encoder[EmailConfig] = deriveEncoder
    implicit val decoderEmailConfig: Decoder[EmailConfig] = deriveDecoder
  }

}