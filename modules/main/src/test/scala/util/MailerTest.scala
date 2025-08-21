// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.util

import cats.effect._
import cats.implicits._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import javax.mail.internet._
import cats.data.NonEmptyList

// monkey test (not a unit test)
object MailerTest extends IOApp {

  val log: Logger[IO] =
    Slf4jLogger.getLoggerFromName(getClass.getName)

  val mail = Mailer.SafeMimeMessage(
      from    = new InternetAddress("announcements@gemini.edu"),
      to      = NonEmptyList.of(new InternetAddress("rob_norris@mac.com")),
      cc      = List(new InternetAddress("rnorris@gemini.edu")),
      subject = "Butter",
      content = "Hello from Scala."
  )

  def run(args: List[String]): IO[ExitCode] =
    Mailer.forProduction[IO](log, "hbfitac-lv1").send(mail).replicateA(3).as(ExitCode.Success)

}

