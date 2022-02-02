// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac
package config

import io.circe._
import io.circe.generic.semiauto._

final case class PerSite[A](gn: A, gs: A) {
  def forSite(site: Site): A =
    site match {
      case GN => gn
      case GS => gs
    }
}

object PerSite {
  implicit def encoderPerSite[A: Encoder]: Encoder[PerSite[A]] = deriveEncoder
  implicit def decoderPerSite[A: Decoder]: Decoder[PerSite[A]] = deriveDecoder

  def unfold[A](f: Site => A): PerSite[A] =
    PerSite(f(GN), f(GS))

}