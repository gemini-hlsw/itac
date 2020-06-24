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
    Mailer.forProduction[IO]("smtp.hi.gemini.edu").sendText(
      from    = new InternetAddress("announcements@gemini.edu"),
      to      = NonEmptyList.of(new InternetAddress("rnorris@mac.com")),
      subject = "Butter",
      message = "Hello from Scala."
    ).as(ExitCode.Success)

}

