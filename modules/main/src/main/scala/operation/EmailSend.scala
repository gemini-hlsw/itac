// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats.data.NonEmptyList
import cats.effect._
import cats.effect.Blocker
import cats.implicits._
import org.typelevel.log4cats.Logger
import itac._
import itac.util.Mailer
import java.nio.charset.Charset
import java.nio.file.{ Files, Path }
import java.util.stream.Collectors
import javax.mail.internet.InternetAddress
import scala.jdk.CollectionConverters._

object EmailSend {

  val FromAddress = new InternetAddress("announcements-gemini@noirlab.edu")
  val Header = s"""^[A-Z]+:\\s+(.*)\\s*""".r

  implicit class StringOps(s: String) {
    def toInternetAddress: InternetAddress =
      new InternetAddress(s.trim)
  }

  def apply[F[_]: Sync](dryRun: Boolean): Operation[F] =
    new Operation[F] {

      // TO:      pi@bar.edu
      // CC:      steve@ngo.edu, bob@gemini.edu
      // SUBJECT: GS-1995-Q-808 Gemini PI Notification
      //
      // Dear 2020B Gemini Principal Investigator,
      // ...
      def readMimeMessage(path: Path): F[Mailer.SafeMimeMessage] = {

        def malformed[A](reason: String): F[A] =
          Sync[F].raiseError(new RuntimeException(s"Malformed email at $path\nReason: $reason"))

        Sync[F].delay(Files.readAllLines(path, Charset.forName("UTF-8")).asScala.toList).flatMap {
          case Header(hTo) :: Header(hCC) :: Header(hSubj) :: bodyLines =>
            Sync[F].delay {
              Mailer.SafeMimeMessage(
                from    = FromAddress,
                to      = NonEmptyList.of(hTo.toInternetAddress),
                cc      = hCC.split(",\\s+").toList.map(_.toInternetAddress),
                subject = hSubj.trim,
                content = bodyLines.mkString("\n")
              )
            }.handleErrorWith(t => malformed(t.getMessage))
          case _ => malformed("Missing or incomplete header")
        }
      }

      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] =
        for {
          dir <- ws.cwd.map(_.resolve("emails")) // TODO: internalize
          m    = if (dryRun) Mailer.forDevelopment[F](log) else Mailer.forProduction[F](log, "localhost")
          _   <- Sync[F].delay { Console.println("This will send emails for real! Enter to continue, ^C to cancel."); Console.in.readLine() } .unlessA(dryRun)
          fs  <- Sync[F].delay(Files.list(dir).collect(Collectors.toList()).asScala.toList) // ugh
          ms  <- fs.traverse(readMimeMessage)
          _   <- ms.traverse_(m.send)
        } yield ExitCode.Success

  }

}