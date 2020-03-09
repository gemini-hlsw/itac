// Copyright (c) 2016-2019 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package itac.operation

import cats.implicits._
import itac.config.QueueConfig
import edu.gemini.tac.qengine.p1.Proposal
import edu.gemini.tac.qengine.p1.Mode
import edu.gemini.tac.qengine.p1.CoreProposal
import edu.gemini.tac.qengine.p1._
import org.apache.velocity.VelocityContext
import java.io.StringWriter
import scala.collection.JavaConverters._
import cats.effect.Sync
import cats.Parallel
import itac.Operation
import cats.effect.{Blocker, ExitCode}
import _root_.io.chrisdavenport.log4cats.Logger
import itac.Workspace
import java.nio.file.Path
import edu.gemini.spModel.core.Site
import itac.EmailTemplateRef
import edu.gemini.spModel.core.Semester
import org.apache.velocity.app.VelocityEngine
import edu.gemini.tac.qengine.api.QueueEngine
import edu.gemini.tac.qengine.api.queue.ProposalQueue
import edu.gemini.model.p1.immutable.TimeAmount
import edu.gemini.tac.qengine.p1.QueueBand.QBand1
import edu.gemini.tac.qengine.p1.QueueBand.QBand2
import edu.gemini.tac.qengine.p1.QueueBand.QBand3
import edu.gemini.tac.qengine.p1.QueueBand.QBand4
import edu.gemini.model.p1.immutable.Band

/**
 * @see Velocity documentation https://velocity.apache.org/engine/2.2/developer-guide.html
 */
object Email {

  // This one's easiest if we uncurry everything.
  def apply[F[_]: Sync: Parallel](
    qe:             QueueEngine,
    siteConfig:     Path,
    rolloverReport: Option[Path]
  ): Operation[F] =
    new AbstractQueueOperation[F](qe, siteConfig, rolloverReport) {

      // The entry
      def run(ws: Workspace[F], log: Logger[F], b: Blocker): F[ExitCode] = {

        // Velocity engine, which takes a bit of work to set up but the computation as a whole is pure.
        val velocity: VelocityEngine = {

          // I feel bad about this but it's the best I could figure out. I need the underlying
          // side-effecting logger from `log` so I can make stupid Velocity use it, but there's no
          // direct accessor. So we will try to pull it out and if it fails we get the normal logger
          // and well, too bad that's what we get. The underlying class is probably
          // io.chrisdavenport.log4cats.slf4j.internal.Slf4jLoggerInternal.Slf4jLogger, which has a
          // `logger` member (and generated accessor `logger()`). In the future maybe someone will
          // use something else, in which case they'll find this comment and work something out.
          val sideEffectingLogger: Option[org.slf4j.Logger] =
            Either.catchNonFatal {
              val getter     = log.getClass.getDeclaredMethod("logger")
              val underlying = getter.invoke(log).asInstanceOf[org.slf4j.Logger]
              underlying
            } .toOption

          val ve = new VelocityEngine
          ve.setProperty("runtime.strict_math",        true)
          ve.setProperty("runtime.strict_mode.enable", true)
          sideEffectingLogger.foreach(ve.setProperty("runtime.log.instance", _))
          ve

        }

        for {
          q  <- computeQueue(ws)
          (ps, qc) = q
          c <- ws.queueConfig(siteConfig)
          // _  <- createSuccessfulEmailsForClassical(velocity, ws, ps, c, qc.queue)
          _  <- createSuccessfulEmailsForQueue(velocity, ws, ps, c, qc.queue)
          // TODO: more!
        } yield ExitCode.Success

      }

      // For now
      type MailMessage = String

      /** Some convenience operations for filtering a list of proposals. */
      implicit class ProposalListOps(ps: List[Proposal]) {

        def classicalProposalsForSite(site: Site): List[Proposal] =
          ps.filter { p =>
              p.site == site           &&
              p.mode == Mode.Classical &&
            !p.isJointComponent
          }

        def successfulQueueProposalsForSite(site: Site, pq: ProposalQueue): List[Proposal] =
          ps.filter { p =>
             pq.positionOf(p).isDefined &&
             p.site == site       &&
             p.mode == Mode.Queue &&
            !p.isJointComponent
          }

      }

      def createPiEmail(velocity: VelocityEngine,ws: Workspace[F], p: Proposal, pq: ProposalQueue): F[MailMessage] = {
        // We have no types to ensure these things, so let's assert to be sure.
        assert(!p.isJointComponent, "Program must not be a joint component.")
        // TODO: assert that p is successful
        for {
          s  <- ws.commonConfig.map(_.semester)
          t  <- ws.readEmailTemplate(EmailTemplateRef.PiSuccessful)
          ps  = velocityBindings(p, s, pq)
          tit = merge(velocity, t.name, t.titleTemplate, ps)
          _  <- Sync[F].delay(println(tit))
          bod = merge(velocity, t.name, t.bodyTemplate, ps)
          _  <- Sync[F].delay(println(bod))
        } yield "ok"
      }

      def createNgoEmails(p: Proposal): F[List[MailMessage]] = {
        p match {
          case c: CoreProposal      => List(s"<email for ${c.ntac.partner.id}>")
          case j: JointProposal     => j.ntacs.map(n =>s"<email for ${n.partner.id}>")
          case _: JointProposalPart => Nil // Don't create mails for joint parts
        }
      } .pure[F]

      // def createSuccessfulEmailsForClassical(velocity: VelocityEngine, ws: Workspace[F], ps: List[Proposal], qc: QueueConfig, pq: ProposalQueue): F[List[MailMessage]] =
      //   ps.classicalProposalsForSite(qc.site).parFlatTraverse { cp =>
      //     (createPiEmail(velocity, ws, cp, pq), createNgoEmails(cp)).parMapN(_ :: _)
      //   }

      def createSuccessfulEmailsForQueue(velocity: VelocityEngine, ws: Workspace[F], ps: List[Proposal], qc: QueueConfig, pq: ProposalQueue): F[List[MailMessage]] =
        ps.successfulQueueProposalsForSite(qc.site, pq).flatTraverse { cp => // TODO: parFlatTraverse
          (createPiEmail(velocity, ws, cp, pq), createNgoEmails(cp)).parMapN(_ :: _)
        }

      /**
       * Given a Velocity template and a map of bindings, evaluate the template and return the
       * generated text, or an indication of why it failed.
       */
      def merge(velocity: VelocityEngine, templateName: String, template: String, bindings: Map[String, AnyRef]): Either[Throwable, String] =
        Either.catchNonFatal {
          val ctx = new VelocityContext(bindings.asJava)
          val out = new StringWriter
          if (!velocity.evaluate(ctx, out, templateName, template)) {
            // It's not at all clear when we get `false` here rather than a thrown exception. It
            // has never come up in testing. But this is here just in case.
            throw new RuntimeException("Velocity evaluation failed (see the log, or re-run with -v debug if there is none!).")
          }
          out.toString
        }

      /**
       * Construct a map of key/value pairs that will be bound to the Velocity context. Our strategy
       * is to define only the keys where values are available (rather than using `null`) and then
       * running in strict reference mode. What this means is, undefined references will throw an
       * exception as Satan intended. Templates can use `#if` to determine whether a key is defined or
       * not, before attempting a dereference.
       * @see strict reference mode https://velocity.apache.org/engine/1.7/user-guide.html#strict-reference-mode
       */
      def velocityBindings(p: Proposal, s: Semester, q: ProposalQueue): Map[String, AnyRef] = {

        println(s"==> ${p.id.reference} - ${q.programId(p).map(_.toString).orEmpty}")

        var mut = scala.collection.mutable.Map.empty[String, AnyRef]
        mut = mut // defeat bogus unused warning

        // bindings that are always present
        mut += "country"             -> p.ntac.partner.fullName

        // What's the difference?
        mut += "ntacRecommendedTime" -> p.ntac.awardedTime.toHours.toString
        mut += "timeAwarded"         -> p.ntac.awardedTime.toHours.toString

        mut += "ntacRefNumber"       -> p.ntac.reference
        mut += "ntacRanking"         -> p.ntac.ranking.format
        mut += "semester"            -> s.toString

        val (partTime, progTime) =
          p.p1proposal.get.observations.foldLeft((TimeAmount.empty, TimeAmount.empty)) { case ((pa, pr), o) =>
            val inBand: Boolean =
              q.positionOf(p).get.band match {
                case QBand1 | QBand2 => o.band == Band.BAND_1_2
                case QBand3 | QBand4 => o.band == Band.BAND_3
              }
            if (o.enabled && inBand) {
              val ts = o.calculatedTimes.get
              (pa |+| ts.partTime, pr |+| ts.progTime)
            } else (pa, pr)
          }

        println(s"Awarded time is ${p.ntac.awardedTime.toHours.toString}, progTime is ${progTime.toHours.format()}, partTime is ${partTime.toHours.format()}")

        // bindings that may be missing
        p.ntac.comment.foreach(v => mut += "ntacComment"  -> v)
        p.piEmail     .foreach(v => mut += "piMail"       -> v)
        p.piName      .foreach(v => mut += "piName"       -> v)
        p.p1proposal  .foreach(p => mut += "progTitle"    -> p.title)
        q.programId(p).foreach(v => mut += "progId"       -> v)


        // Not implemented yet
        // mut += "geminiComment"       -> null
        // mut += "geminiContactEmail"  -> null
        // mut += "geminiId"            -> null
        // mut += "jointInfo"           -> null
        // mut += "jointTimeContribs"   -> null
        // mut += "ntacSupportEmail"    -> null // need to add to Partner based on c
        // mut += "progKey"             -> null
        // mut += "queueBand"           -> null

        // Done
        mut.toMap

      }

    }

}