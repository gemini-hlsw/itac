// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.codec

import cats.implicits._
import io.circe._
import java.text.ParseException
import edu.gemini.spModel.core.Semester

trait SemesterCodec {

  implicit val EncoderSemester: Encoder[Semester] =
    Encoder[String].contramap(_.toString)

  implicit val DecoderSemester: Decoder[Semester] =
    Decoder[String].emap { s =>
      Either.catchOnly[ParseException](Semester.parse(s)).leftMap(_.getMessage)
    }

}

object semester extends SemesterCodec