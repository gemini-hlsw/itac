// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import cats.implicits._
import io.circe.Encoder
import io.circe.Decoder

trait TokensCodecs {

  def encodeTokens[A](encode: A => String): Encoder[List[A]] =
    Encoder[String].contramap(as => as.map(encode).intercalate(" "))

  def decodeTokens[A](decode: String => Either[String, A]): Decoder[List[A]] =
    Decoder[String].emap("\\S+".r.findAllIn(_).toList.traverse(decode))

}

object tokens extends TokensCodecs