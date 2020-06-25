// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.util

import cats.effect._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import javax.mail.internet._
import cats.data.NonEmptyList

// monkey test (not a unit test)
object MailerTest extends IOApp {

  implicit val log: Logger[IO] =
    Slf4jLogger.getLoggerFromName(getClass.getName)

  def run(args: List[String]): IO[ExitCode] =
    Mailer.forProduction[IO]("hbfitac-lv1").sendText(
      from    = new InternetAddress("announcements@gemini.edu"),
      to      = NonEmptyList.of(new InternetAddress("rob_norris@mac.com")),
      subject = "Butter",
      message = "Hello from Scala."
    ).as(ExitCode.Success)

}

