// Copyright (c) 2016-2022 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.util

import cats.effect._
import cats.implicits._

import javax.mail._
import javax.mail.Message.RecipientType.{ TO, CC }
import javax.mail.internet._
import io.chrisdavenport.log4cats.Logger
import cats.data.NonEmptyList
import cats.Apply

sealed trait Mailer[F[_]] {
  def send(m: Mailer.SafeMimeMessage): F[Unit]
}

object Mailer {

  case class SafeMimeMessage(
    from:    InternetAddress,
    to:      NonEmptyList[InternetAddress],
    cc:      List[InternetAddress],
    subject: String,
    content: String
  ) {
    def toAddressString: String = to.map(_.toString).intercalate(", ")
    def ccAddressString: String = cc.map(_.toString).intercalate(", ")
    def toMimeMessage(session: Session): MimeMessage = {
      val m = new MimeMessage(session)
      m.setFrom(from)
      to.toList.foreach(m.addRecipient(TO, _))
      cc.foreach(m.addRecipient(CC, _))
      m.setSubject(subject)
      m.setContent(content, "text/plain")
      m
    }
  }

  def forProduction[F[_]: Sync](log: Logger[F], smtpHost: String): Mailer[F] =
    new Mailer[F] {

      // There's no way to close a Session so I guess it's stateless?
      val session = {
        val props = new java.util.Properties
        props.put("mail.transport.protocol", "smtp")
        props.put("mail.smtp.host", smtpHost)
        Session.getInstance(props, null /* no authenticator */)
      }

      override def send(m: SafeMimeMessage): F[Unit] = {
        Sync[F].delay(Transport.send(m.toMimeMessage(session))) *>
        log.warn(s"""Successfully sent "${m.subject}" to ${m.toAddressString}.""")
      } .handleErrorWith(t => log.warn(t)(s"""Failed to send "${m.subject}" to ${m.toAddressString}."""))

    }

  def forDevelopment[F[_]: Apply](log: Logger[F]): Mailer[F] =
    new Mailer[F] {
      override def send(m: SafeMimeMessage): F[Unit] =
        log.info(s"""Successfully pretended to send "${m.subject}" to ${m.toAddressString}.""")
    }

}


