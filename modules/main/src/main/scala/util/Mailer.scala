// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.util

import cats.effect._
import cats.implicits._

import javax.mail._
import javax.mail.Message.RecipientType.TO
import javax.mail.internet._
import io.chrisdavenport.log4cats.Logger
import cats.data.NonEmptyList

sealed trait Mailer[F[_]] {

  def sendText(
    from:    InternetAddress,
    to:      NonEmptyList[InternetAddress],
    subject: String,
    message: String
  ): F[Unit] =
    send(Mailer.SafeMimeMessage(from, to, subject, message))

  protected def log(m: Mailer.SafeMimeMessage)(
    implicit ev: Logger[F]
  ): F[Unit] =
    Logger[F].warn(
        s"""|
            |From...: ${m.from}
            |To.....: ${m.toAddressString}
            |Subject: ${m.subject}
            |
            |${m.content}
            |""".stripMargin
      )

  protected def send(m: Mailer.SafeMimeMessage): F[Unit]

}

object Mailer {

  sealed trait Type extends Product with Serializable
  object Type {
    case object Development extends Type
    case object Production  extends Type
    def fromString(s: String): Option[Type] =
      Some(s) collect {
        case "development" => Development
        case "production"  => Production
      }
  }

  case class SafeMimeMessage(
    from:    InternetAddress,
    to:      NonEmptyList[InternetAddress],
    subject: String,
    content: String
  ) {
    def toAddressString: String = to.map(_.toString).intercalate(", ")
    def toMimeMessage(smtpHost: String): MimeMessage = {
      val props = {
        val p = new java.util.Properties
        p.put("mail.transport.protocol", "smtp")
        p.put("mail.smtp.host", smtpHost)
        p
      }
      val m = new MimeMessage(Session.getInstance(props, null))
      m.setFrom(from)
      to.toList.foreach(m.addRecipient(TO, _))
      m.setSubject(subject)
      m.setContent(content, "text/plain")
      m
    }
  }

  def forProduction[F[_]: Sync: Logger](smtpHost: String): Mailer[F] =
    new Mailer[F] {
      protected final def sendSMTP(m: SafeMimeMessage): F[Unit] = {
        val to   = m.toAddressString
        val send = Sync[F].delay(Transport.send(m.toMimeMessage(smtpHost))) *>
                   Logger[F].info(s"""Successfully sent mail "${m.subject}" to $to.""")
        send.handleErrorWith(t => Logger[F].warn(t)(s"""Failed to send mail "${m.subject}" to $to."""))
      }
      override def send(m: SafeMimeMessage): F[Unit] =
        log(m) *> sendSMTP(m)
    }


  def forDevelopment[F[_]: Logger]: Mailer[F] =
    new Mailer[F] {
      override def send(m: SafeMimeMessage): F[Unit] = log(m)
    }

  def ofType[F[_]: Sync: Logger](t: Type, smtpHost: String): Mailer[F] =
    t match {
      case Type.Production  => Mailer.forProduction(smtpHost)
      case Type.Development => Mailer.forDevelopment
    }

}


